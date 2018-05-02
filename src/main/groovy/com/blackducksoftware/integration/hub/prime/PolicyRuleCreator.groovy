package com.blackducksoftware.integration.hub.prime

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.enumeration.PolicyRuleConditionOperatorType
import com.blackducksoftware.integration.hub.api.generated.component.PolicyRuleExpressionSetView
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView
import com.blackducksoftware.integration.hub.api.generated.view.PolicyRuleView
import com.blackducksoftware.integration.hub.api.generated.view.PolicyRuleViewV2
import com.blackducksoftware.integration.hub.api.view.MetaHandler
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.exception.DoesNotExistException
import com.blackducksoftware.integration.hub.service.ComponentService
import com.blackducksoftware.integration.hub.service.HubService
import com.blackducksoftware.integration.hub.service.PolicyRuleService
import com.blackducksoftware.integration.hub.service.model.PolicyRuleExpressionSetBuilder

@Component
class PolicyRuleCreator {
    public static final String NO_FILEUPLOAD_1_2_1 = 'No Commons FileUpload 1.2.1'

    @Autowired
    PolicyRuleService policyRuleService

    @Autowired
    ComponentService componentService

    @Autowired
    HubService hubService

    @Autowired
    MetaHandler metaHandler

    @Autowired
    ExternalIdFactory externalIdFactory

    PolicyRuleViewV2 createNoCommonsFileUpload() {
        PolicyRuleViewV2 noCommonsFileUploadPolicyView = null
        PolicyRuleView existingPolicyRule = null;
        try {
            existingPolicyRule = policyRuleService.getPolicyRuleViewByName(NO_FILEUPLOAD_1_2_1)
            noCommonsFileUploadPolicyView = policyRuleService.getPolicyRuleViewV2(existingPolicyRule);
        } catch (DoesNotExistException e) {
            ExternalId commonsFileUploadExternalId = externalIdFactory.createMavenExternalId('commons-fileupload', 'commons-fileupload', '1.2.1')
            ComponentVersionView componentVersionView =  componentService.getComponentVersion(commonsFileUploadExternalId)

            PolicyRuleExpressionSetBuilder builder = new PolicyRuleExpressionSetBuilder(metaHandler)
            builder.addComponentVersionCondition(PolicyRuleConditionOperatorType.EQ, componentVersionView)
            PolicyRuleExpressionSetView expressionSet = builder.createPolicyRuleExpressionSetView()

            noCommonsFileUploadPolicyView = new PolicyRuleViewV2()
            noCommonsFileUploadPolicyView.name = 'No Commons FileUpload 1.2.1'
            noCommonsFileUploadPolicyView.enabled = true
            noCommonsFileUploadPolicyView.overridable = true
            noCommonsFileUploadPolicyView.expression = expressionSet

            policyRuleService.createPolicyRule(noCommonsFileUploadPolicyView);
        }
        return noCommonsFileUploadPolicyView
    }
}
