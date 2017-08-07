/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.AbstractAzureMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Paths;

public abstract class AbstractFunctionMojo extends AbstractAzureMojo {
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    @Parameter(property = "resourceGroup", required = true)
    protected String resourceGroup;

    @Parameter(property = "appName", required = true)
    protected String appName;

    @Parameter(property = "region", defaultValue = "westus")
    protected String region;

    public String getFinalName() {
        return finalName;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    public String getRegion() {
        return region;
    }

    public String getDeploymentStageDirectory() {
        return Paths.get(getBuildDirectoryAbsolutePath(),
                "azure-functions",
                getAppName()).toString();
    }

    public FunctionApp getFunctionApp() {
        try {
            return getAzureClient().appServices().functionApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (Exception e) {
            // Swallow exception
        }
        return null;
    }
}
