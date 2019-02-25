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
package com.synopsys.integration.blackduck.prime

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder
import com.synopsys.integration.blackduck.prime.service.ConfigureService
import com.synopsys.integration.blackduck.prime.service.UserService
import com.synopsys.integration.blackduck.prime.service.VersionService
import com.synopsys.integration.blackduck.service.BlackDuckService
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import org.apache.commons.lang3.EnumUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean

@SpringBootApplication
class Application implements CommandLineRunner {
    private final Logger logger = LoggerFactory.getLogger(Application.class)

    public static final EnumSet<Action> DEFAULT_ACTIONS = EnumSet.allOf(Action.class)

    @Value('${blackduck.url:}')
    private String blackduckUrl

    @Value('${blackduck.timeout:120}')
    private String blackduckTimeout

    @Value('${blackduck.trust.cert:false}')
    private String blackduckTrustCert

    @Value('${blackduck.username:}')
    private String blackduckUsername

    @Value('${blackduck.password:}')
    private String blackduckPassword

    @Value('${blackduck.api.token:}')
    private String blackduckApiToken

    @Autowired
    private ConfigureService configureService

    @Autowired
    private UserService userService

    @Autowired
    private VersionService versionService

    static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args)
    }

    @Override
    void run(String... args) {
        List<Action> providedActions = args
                .collect { it.toUpperCase() }
                .findAll { EnumUtils.isValidEnum(Action.class, it) }
                .collect { Action.valueOf(it) }

        EnumSet<Action> actionsToPerform = DEFAULT_ACTIONS;
        if (providedActions) {
            logger.info('Found provided actions.');
            actionsToPerform = EnumSet.of(providedActions)
        }

        logger.info("Attempting the following actions: ${StringUtils.join(actionsToPerform.collect { it.name() })}")
        if (actionsToPerform.contains(Action.REGISTER)) {
            configureService.applyRegistrationId()
        }

        if (actionsToPerform.contains(Action.ACCEPT_EULA)) {
            configureService.acceptEndUserLicenseAgreement()
        }

        if (actionsToPerform.contains(Action.VERSION)) {
            String currentVersion = versionService.retrieveCurrentVersion()
            logger.info("Current version: ${currentVersion}")
        }

        if (actionsToPerform.contains(Action.ADD_USER)) {
            Optional<String> userUrl = userService.addUserFromProperties()
            if (userUrl.isPresent()) {
                logger.info("New user can be viewed here: ${userUrl.get()}")
            } else {
                logger.warn('No new user was added.')
            }
        }

        if (actionsToPerform.contains(Action.USER_COUNT)) {
            int totalUserCount = userService.calculateUserCount()
            logger.info("Total User Count: ${totalUserCount}")
        }

        logger.info('Completed all actions successfully.')
    }

    @Bean
    IntLogger intLogger() {
        new Slf4jIntLogger(logger)
    }

    @Bean
    BlackDuckServerConfig blackDuckServerConfig() {
        BlackDuckServerConfigBuilder builder = BlackDuckServerConfig.newBuilder()
        builder.setLogger(intLogger())

        builder.setUrl(blackduckUrl)
        builder.setTimeout(blackduckTimeout)
        builder.setTrustCert(blackduckTrustCert)
        builder.setUsername(blackduckUsername)
        builder.setPassword(blackduckPassword)
        builder.setApiToken(blackduckApiToken)

        builder.build()
    }

    @Bean
    BlackDuckServicesFactory blackDuckServicesFactory() {
        blackDuckServerConfig().createBlackDuckServicesFactory(intLogger())
    }

    @Bean
    BlackDuckService blackDuckService() {
        blackDuckServicesFactory().createBlackDuckService()
    }

}
