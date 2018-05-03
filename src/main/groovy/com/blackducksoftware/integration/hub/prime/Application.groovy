package com.blackducksoftware.integration.hub.prime

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean

import com.blackducksoftware.integration.hub.api.generated.view.PolicyRuleViewV2
import com.blackducksoftware.integration.hub.api.view.MetaHandler
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.configuration.HubServerConfig
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.notification.NotificationResults
import com.blackducksoftware.integration.hub.notification.content.NotificationContent
import com.blackducksoftware.integration.hub.rest.RestConnection
import com.blackducksoftware.integration.hub.service.CodeLocationService
import com.blackducksoftware.integration.hub.service.ComponentService
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.hub.service.NotificationService
import com.blackducksoftware.integration.hub.service.PolicyRuleService
import com.blackducksoftware.integration.hub.service.ProjectService
import com.blackducksoftware.integration.hub.service.model.ProjectVersionWrapper
import com.blackducksoftware.integration.log.IntLogger
import com.blackducksoftware.integration.log.Slf4jIntLogger

@SpringBootApplication
class Application {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    public static final long FIVE_SECONDS = 5*1000;

    @Autowired
    ProjectCreator projectCreator

    @Autowired
    PolicyRuleCreator policyRuleCreator

    @Autowired
    BomUpdater bomUpdater

    @Autowired
    NotificationService notificationService

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @PostConstruct
    void init() {
        def projectName = 'ek-hub-prime'
        def projectVersionName = '1.0.0'
        ProjectVersionWrapper projectVersionWrapper = projectCreator.createProject(projectName, projectVersionName)
        String retrievedProjectName = projectVersionWrapper.getProjectView().name;
        String retrievedProjectVersionName = projectVersionWrapper.getProjectVersionView().versionName;
        logger.info("Successfully retrieved project: ${retrievedProjectName}/${retrievedProjectVersionName}")

        PolicyRuleViewV2 noCommonsFileUploadPolicyRule = policyRuleCreator.createNoCommonsFileUpload();
        logger.info("Successfully retrieved policy rule: ${noCommonsFileUploadPolicyRule.name}");

        Date currentNotificationDate = notificationService.getLatestNotificationDate();
        bomUpdater.addCommonsFileUpload()

        ZonedDateTime utcTime = ZonedDateTime.ofInstant(currentNotificationDate.toInstant(), ZoneOffset.UTC);
        ZonedDateTime nextMillisecond = utcTime.plus(1, ChronoUnit.MILLIS);
        ZonedDateTime nextHour = ZonedDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS);
        Date nextNotificationStart = Date.from(nextMillisecond.toInstant());
        Date nextNotificationEnd = Date.from(nextHour.toInstant());
        String startDateString = RestConnection.formatDate(nextNotificationStart);
        String endDateString = RestConnection.formatDate(nextNotificationEnd);

        logger.info("Finding notifications for ${startDateString} to ${endDateString}")
        NotificationResults notificationResults = notificationService.getAllNotificationResults(nextNotificationStart, nextNotificationEnd)
        while (!validNotifications(notificationResults)) {
            logger.info("Still haven't found valid notifications for ${nextNotificationStart} - ${nextNotificationEnd}.")
            Thread.sleep(FIVE_SECONDS)
            notificationResults = notificationResults = notificationService.getAllNotificationResults(nextNotificationStart, nextNotificationEnd)
        }

        // FIXME might not have to do this - another call to notificationService.getLatestNotificationDate() might suffice
        Instant latestCreated = nextNotificationStart.toInstant();
        notificationResults.notificationContentItems.each { commonNotification ->
            Instant createdInstant = commonNotification.createdAt.toInstant()
            if (createdInstant.isAfter(latestCreated)) {
                latestCreated = createdInstant;
            }
        }
        latestCreated = latestCreated.plus(1, ChronoUnit.MILLIS);
        nextNotificationEnd = Date.from(latestCreated)
        endDateString = RestConnection.formatDate(nextNotificationEnd);
        logger.info("Found notifications for ${startDateString} to ${endDateString}")

        bomUpdater.removeCommonsFileUpload()

        projectCreator.createProject('notifications-policy_violation_and_vulnerability', 'startDate', startDateString)
        projectCreator.createProject('notifications-policy_violation_and_vulnerability', 'endDate', endDateString)
    }

    private boolean validNotifications(NotificationResults notificationResults) {
        if (notificationResults.notificationContentItems.size() != 2) {
            return false
        }

        boolean foundFileUploadVulnerability = false
        boolean foundFileUploadPolicyViolation = false
        notificationResults.notificationContentItems.each { commonNotification ->
            NotificationContent notificationContent = commonNotification.content
            notificationContent.notificationContentDetails.each { notificationContentDetail ->
                if (notificationContentDetail.getComponentName().isPresent() && notificationContentDetail.getComponentVersionName().isPresent()) {
                    String componentName = notificationContentDetail.getComponentName().get();
                    String componentVersionName = notificationContentDetail.getComponentVersionName().get();
                    if ('Apache Commons FileUpload'.equals(componentName) && '1.2.1'.equals(componentVersionName)) {
                        if (notificationContent.providesVulnerabilityDetails()) {
                            foundFileUploadVulnerability = true
                        } else if (notificationContent.providesPolicyDetails()) {
                            foundFileUploadPolicyViolation = true
                        }
                    }
                }
            }
        }

        return foundFileUploadVulnerability && foundFileUploadPolicyViolation
    }

    @Bean
    IntLogger intLogger() {
        new Slf4jIntLogger(logger)
    }

    @Bean
    HubServerConfig hubServerConfig() {
        HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
        hubServerConfigBuilder.setHubUrl(System.getenv().get('BLACKDUCK_HUB_URL'))
        hubServerConfigBuilder.setUsername(System.getenv().get('BLACKDUCK_HUB_USERNAME'))
        hubServerConfigBuilder.setPassword(System.getenv().get('BLACKDUCK_HUB_PASSWORD'))
        hubServerConfigBuilder.setTimeout(120)
        hubServerConfigBuilder.setAlwaysTrustServerCertificate(true)
        hubServerConfigBuilder.setLogger(intLogger())

        HubServerConfig hubServerConfig = hubServerConfigBuilder.build()
        hubServerConfig
    }

    @Bean
    RestConnection restConnection() {
        hubServerConfig().createRestConnection(intLogger())
    }

    @Bean
    HubServicesFactory hubServicesFactory() {
        new HubServicesFactory(restConnection())
    }

    @Bean
    ExternalIdFactory externalIdFactory() {
        new ExternalIdFactory()
    }

    @Bean
    MetaHandler metaHandler() {
        new MetaHandler(intLogger())
    }

    @Bean
    HubService hubService() {
        hubServicesFactory().createHubService()
    }

    @Bean
    ProjectService projectService() {
        hubServicesFactory().createProjectService()
    }

    @Bean
    CodeLocationService codeLocationService() {
        hubServicesFactory().createCodeLocationService()
    }

    @Bean
    ComponentService componentService() {
        hubServicesFactory().createComponentService()
    }

    @Bean
    PolicyRuleService policyRuleService() {
        hubServicesFactory().createPolicyRuleService()
    }

    @Bean
    NotificationService notificationService() {
        hubServicesFactory().createNotificationService()
    }
}
