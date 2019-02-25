/*
 * blackduck-prime
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.prime.service

import com.synopsys.integration.blackduck.api.generated.component.EndUserLicenseAgreementAction
import com.synopsys.integration.blackduck.api.generated.component.RegistrationRequest
import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery
import com.synopsys.integration.blackduck.service.BlackDuckService
import com.synopsys.integration.log.IntLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ConfigureService {
    @Value('${registration.id:}')
    private String registrationId

    @Autowired
    IntLogger intLogger

    @Autowired
    BlackDuckService blackDuckService

    void acceptEndUserLicenseAgreement() {
        intLogger.info('Attempting to accept the end user license agreement...')
        def endUserLicenseAgreementAction = new EndUserLicenseAgreementAction()
        endUserLicenseAgreementAction.accept = true
        endUserLicenseAgreementAction.acceptEndUserLicense = true

        blackDuckService.post(ApiDiscovery.ENDUSERLICENSEAGREEMENT_LINK, endUserLicenseAgreementAction)
        intLogger.info('Successfully accepted the end user license agreement.')
    }

    void applyRegistrationId() {
        if (!registrationId) {
            intLogger.warn('No registration id was provided. If you want to update the registration id, please provide the \'registration.id\' property.')
            return
        }

        intLogger.info('Attempting to update the registration id...')
        def registrationRequest = new RegistrationRequest()
        registrationRequest.registrationId = registrationId

        blackDuckService.post(ApiDiscovery.REGISTRATION_LINK, registrationRequest)
        intLogger.info('Successfully updated the registration id.')
    }

}
