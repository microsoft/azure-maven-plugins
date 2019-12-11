/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Deployment {
    private static final String DEFAULT_DIRECTORY = "${project.basedir}/target";
    private static final String DEFAULT_INCLUDE = "*.%s";
    private static final String DEFAULT_DEPLOYTYPE = "jar";

    protected List<Resource> resources = Collections.emptyList();

    public List<Resource> getResources() {
        return this.resources;
    }

    public void setResources(final List<Resource> value) {
        this.resources = value;
    }

    public static Deployment getDefaultDeploymentConfiguration(String deployType) {
        final Deployment result = new Deployment();
        final Resource resource = new Resource();
        deployType = StringUtils.isEmpty(deployType) ? DEFAULT_DEPLOYTYPE : deployType;

        resource.setDirectory(DEFAULT_DIRECTORY);
        resource.addInclude(String.format(DEFAULT_INCLUDE, deployType));

        result.setResources(Arrays.asList(resource));
        return result;
    }
}
