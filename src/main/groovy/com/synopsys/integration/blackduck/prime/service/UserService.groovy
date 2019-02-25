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

import com.synopsys.integration.blackduck.api.generated.component.UserRequest
import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery
import com.synopsys.integration.blackduck.api.generated.view.UserView
import com.synopsys.integration.blackduck.service.BlackDuckPageResponse
import com.synopsys.integration.blackduck.service.BlackDuckService
import com.synopsys.integration.log.IntLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class UserService {
    @Value('${new.user.username:}')
    private String newUserUsername

    @Value('${new.user.password:}')
    private String newUserPassword

    @Value('${new.user.first.name:}')
    private String newUserFirstName

    @Value('${new.user.last.name:}')
    private String newUserLastName

    @Value('${new.user.email:}')
    private String newUserEmail

    @Autowired
    IntLogger intLogger

    @Autowired
    BlackDuckService blackDuckService

    int calculateUserCount() {
        BlackDuckPageResponse<UserView> singlePageOfUsers = blackDuckService.getPageResponses(ApiDiscovery.USERS_LINK_RESPONSE, false)

        singlePageOfUsers.totalCount
    }

    Optional<String> addUserFromProperties() {
        if (newUserUsername && newUserPassword && newUserFirstName && newUserLastName) {
            intLogger.info('Properties were sufficient to create a user.')
            UserRequest userRequest = createUserRequest(newUserUsername, newUserPassword, newUserFirstName, newUserLastName)
            userRequest.email = newUserEmail

            Optional.of(addNewUser(userRequest))
        } else {
            intLogger.warn('Properties were NOT sufficient to create a user: username, first name, last name, and password are all required.')
            intLogger.warn("Please provide 'new.user.username', 'new.user.password', 'new.user.first.name', 'new.user.last.name', and optionally, 'new.user.email'.")
        }

        Optional.empty()
    }

    String addNewUser(UserRequest userRequest) {
        intLogger.info("Adding new user: ${userRequest.userName}")
        String userViewUrl = blackDuckService.post(ApiDiscovery.USERS_LINK, userRequest)
        intLogger.info("Successfully added user: ${userRequest.userName} (${userViewUrl})")

        userViewUrl
    }

    UserRequest createUserRequest(String username, String password, String firstName, String lastName) {
        def userRequest = new UserRequest()
        userRequest.userName = username
        userRequest.firstName = firstName
        userRequest.lastName = lastName
        userRequest.password = password
        userRequest.active = true

        userRequest
    }

}
