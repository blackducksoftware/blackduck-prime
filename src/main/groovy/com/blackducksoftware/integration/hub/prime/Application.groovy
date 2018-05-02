package com.blackducksoftware.integration.hub.prime

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

import com.blackducksoftware.integration.hub.api.generated.view.NotificationView
import com.blackducksoftware.integration.hub.api.generated.view.PolicyRuleViewV2
import com.blackducksoftware.integration.hub.api.view.MetaHandler
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.configuration.HubServerConfig
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder
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
        println currentNotificationDate
        ZonedDateTime utcTime = ZonedDateTime.ofInstant(currentNotificationDate.toInstant(), ZoneOffset.UTC);
        ZonedDateTime nextMillisecond = utcTime.plus(1, ChronoUnit.MILLIS);
        ZonedDateTime nextHour = ZonedDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS);
        Date nextNotificationStart = Date.from(nextMillisecond.toInstant());
        Date nextNotificationEnd = Date.from(nextHour.toInstant());
        bomUpdater.addCommonsFileUpload()

        List<NotificationView> notifications = notificationService.getAllNotifications(nextNotificationStart, nextNotificationEnd)
        while (notifications.size() < 2) {
            logger.info("Still haven't found new notifications for ${nextNotificationStart} - ${nextNotificationEnd}.")
            Thread.sleep(FIVE_SECONDS)
            notifications = notificationService.getAllNotifications(nextNotificationStart, nextNotificationEnd)
        }
        currentNotificationDate = notificationService.getLatestNotificationDate()
        println currentNotificationDate

        bomUpdater.removeCommonsFileUpload()
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
