/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

import org.apache.maven.plugin.MojoExecutionException;

import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;

public class LinuxRuntimeHandlerImpl implements RuntimeHandler {

    private static final String NOT_SUPPORTED_IMAGE = "The image: '%s' is not supported.";
    private static final String IMAGE_NOT_GIVEN = "Image name is not specified.";

    private AbstractWebAppMojo mojo;

    public LinuxRuntimeHandlerImpl(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRuntime() throws Exception {
        return WebAppUtils.defineApp(mojo)
                .withNewLinuxPlan(mojo.getPricingTier())
                .withBuiltInImage(this.getJavaRunTimeStack(mojo.getLinuxRuntime()));
    }

    @Override
    public WebApp.Update updateAppRuntime(WebApp app) throws Exception {
        WebAppUtils.assureLinuxWebApp(app);

        return app.update().withBuiltInImage(this.getJavaRunTimeStack(mojo.getLinuxRuntime()));
    }

    private RuntimeStack getJavaRunTimeStack(String imageName) throws MojoExecutionException {
        if (isNotEmpty(imageName)) {
            if (imageName.equalsIgnoreCase(RuntimeStack.TOMCAT_8_5_JRE8.toString())) {
                return RuntimeStack.TOMCAT_8_5_JRE8;
            } else if (imageName.equalsIgnoreCase(RuntimeStack.TOMCAT_9_0_JRE8.toString())) {
                return RuntimeStack.TOMCAT_9_0_JRE8;
            } else {
                throw new MojoExecutionException(String.format(NOT_SUPPORTED_IMAGE, imageName));
            }
        }
        throw new MojoExecutionException(IMAGE_NOT_GIVEN);
    }
}
