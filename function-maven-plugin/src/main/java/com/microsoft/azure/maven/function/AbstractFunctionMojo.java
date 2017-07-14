/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.AbstractAzureMojo;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

public abstract class AbstractFunctionMojo extends AbstractAzureMojo {
    @Parameter(property = "resourceGroup", required = true)
    protected String resourceGroup;

    @Parameter(property = "appName", required = true)
    protected String appName;

    @Parameter(property = "region", defaultValue = "westus")
    protected String region;

    @Parameter(property = "functionName", required = true)
    protected String functionName;

    @Parameter(property = "resources")
    protected List<Resource> resources;

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    public String getRegion() {
        return region;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<Resource> getResources() {
        return resources;
    }
}
