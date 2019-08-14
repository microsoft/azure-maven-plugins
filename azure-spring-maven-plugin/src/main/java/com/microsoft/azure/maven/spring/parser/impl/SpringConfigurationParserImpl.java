/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.parser.impl;

import com.microsoft.azure.maven.spring.AbstractSpringMojo;
import com.microsoft.azure.maven.spring.SpringConfiguration;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParser;

public class SpringConfigurationParserImpl implements SpringConfigurationParser {
    @Override
    public SpringConfiguration parse(AbstractSpringMojo springMojo) {
        return new SpringConfiguration()
                .withAppName(springMojo.getAppName())
                .withClusterName(springMojo.getClusterName())
                .withDeployment(springMojo.getDeployment())
                .withRuntimeVersion(springMojo.getRuntimeVersion())
                .withPublic(springMojo.isPublic())
                .withSubscriptionId(springMojo.getSubscriptionId());
    }
}
