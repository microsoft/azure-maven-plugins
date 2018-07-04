/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {
    public static final String AZURE_FUNCTIONS = "azure-functions";

    //region Properties

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    /**
     * Skip execution.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.skip", defaultValue = "false")
    protected boolean skip;

    //endregion

    //region Getter

    @Override
    protected boolean isSkipMojo() {
        return skip;
    }

    public String getFinalName() {
        return finalName;
    }

    public String getDeploymentStageDirectory() {
        return Paths.get(getBuildDirectoryAbsolutePath(),
                AZURE_FUNCTIONS,
                getAppName()).toString();
    }

    public FunctionApp getFunctionApp() throws AzureAuthFailureException {
        try {
            return getAzureClient().appServices().functionApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (AzureAuthFailureException authEx) {
            throw authEx;
        } catch (Exception ex) {
            // Swallow exception for non-existing Azure Functions
        }
        return null;
    }

    //endregion
}
