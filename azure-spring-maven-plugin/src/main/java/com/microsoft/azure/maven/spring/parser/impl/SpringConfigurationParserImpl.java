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
                .setAppName(springMojo.getAppName())
                .setClusterName(springMojo.getClusterName())
                .setDeployment(springMojo.getDeployment())
                .setJavaVersion(springMojo.getJavaVersion())
                .setPublic(springMojo.isPublic())
                .setResourceGroup(springMojo.getResourceGroup())
                .setSubscriptionId(springMojo.getSubscriptionId());
    }
}
