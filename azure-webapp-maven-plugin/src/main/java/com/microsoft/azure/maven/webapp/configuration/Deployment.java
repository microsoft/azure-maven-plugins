/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.maven.model.DeploymentResource;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class Deployment {
    private static final String DEFAULT_DIRECTORY = "${project.basedir}/target";
    private static final String DEFAULT_INCLUDE = "*.%s";
    private static final String DEFAULT_DEPLOYTYPE = "jar";

    protected List<DeploymentResource> resources = Collections.emptyList();

    public List<DeploymentResource> getResources() {
        return this.resources;
    }

    public void setResources(final List<DeploymentResource> value) {
        this.resources = value;
    }

    public static Deployment getDefaultDeploymentConfiguration(String deployType) {
        final Deployment result = new Deployment();
        final DeploymentResource resource = new DeploymentResource();

        resource.setDirectory(DEFAULT_DIRECTORY);
        resource.addInclude(String.format(DEFAULT_INCLUDE, StringUtils.firstNonBlank(deployType, DEFAULT_DEPLOYTYPE)));
        result.setResources(Collections.singletonList(resource));
        return result;
    }
}
