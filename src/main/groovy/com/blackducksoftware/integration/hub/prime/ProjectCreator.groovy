package com.blackducksoftware.integration.hub.prime

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.generated.component.ProjectRequest
import com.blackducksoftware.integration.hub.api.generated.enumeration.ProjectVersionDistributionType
import com.blackducksoftware.integration.hub.api.generated.enumeration.ProjectVersionPhaseType
import com.blackducksoftware.integration.hub.service.ProjectService
import com.blackducksoftware.integration.hub.service.model.ProjectRequestBuilder
import com.blackducksoftware.integration.hub.service.model.ProjectVersionWrapper

@Component
class ProjectCreator {
    @Autowired
    ProjectService projectService

    ProjectVersionWrapper createProject(String projectName, String projectVersionName) {
        ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder()

        return populateAndSubmit(projectRequestBuilder, projectName, projectVersionName)
    }

    ProjectVersionWrapper createProject(String projectName, String projectVersionName, String nickname) {
        ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder()
        projectRequestBuilder.versionNickname = nickname

        return populateAndSubmit(projectRequestBuilder, projectName, projectVersionName)
    }

    private ProjectVersionWrapper populateAndSubmit(ProjectRequestBuilder projectRequestBuilder, String projectName, String projectVersionName) {
        projectRequestBuilder.projectName = projectName
        projectRequestBuilder.versionName = projectVersionName
        projectRequestBuilder.phase = ProjectVersionPhaseType.DEVELOPMENT.name()
        projectRequestBuilder.distribution = ProjectVersionDistributionType.OPENSOURCE.name()

        ProjectRequest projectRequest = projectRequestBuilder.build()
        ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersionAndCreateIfNeeded(projectRequest)
        return projectVersionWrapper
    }
}