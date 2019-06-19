/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import org.apache.maven.plugin.MojoExecutionException;

public class V2NoValidationConfigurationParser extends V2ConfigurationParser {

    public V2NoValidationConfigurationParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    protected String getAppName() throws MojoExecutionException {
        return validateConfiguration(validator.validateAppName()) ? super.getAppName() : mojo.getAppName();
    }

    @Override
    protected String getResourceGroup() throws MojoExecutionException {
        return validateConfiguration(validator.validateResourceGroup()) ?
                super.getResourceGroup() : mojo.getResourceGroup();
    }

    @Override
    protected OperatingSystemEnum getOs() throws MojoExecutionException {
        return validateConfiguration(validator.validateOs()) ? super.getOs() : null;
    }

    @Override
    protected Region getRegion() throws MojoExecutionException {
        return validateConfiguration(validator.validateRegion()) ? super.getRegion() : null;
    }

    @Override
    protected RuntimeStack getRuntimeStack() throws MojoExecutionException {
        return validateConfiguration(validator.validateRuntimeStack()) ? super.getRuntimeStack() : null;
    }

    @Override
    protected String getImage() throws MojoExecutionException {
        return validateConfiguration(validator.validateImage()) ? super.getImage() : null;
    }

    @Override
    protected JavaVersion getJavaVersion() throws MojoExecutionException {
        return validateConfiguration(validator.validateJavaVersion()) ? super.getJavaVersion() : null;
    }

    @Override
    protected WebContainer getWebContainer() throws MojoExecutionException {
        return validateConfiguration(validator.validateWebContainer()) ? super.getWebContainer() : null;
    }

    protected boolean validateConfiguration(String errorMessage) {
        if (errorMessage != null) {
            mojo.getLog().warn(errorMessage);
        }
        return errorMessage == null;
    }
}
