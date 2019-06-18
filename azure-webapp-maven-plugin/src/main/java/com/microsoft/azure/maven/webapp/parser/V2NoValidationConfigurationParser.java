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
import org.apache.maven.plugin.MojoExecutionException;

public class V2NoValidationConfigurationParser extends V2ConfigurationParser {

    public V2NoValidationConfigurationParser(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    protected String getAppName() {
        try {
            return super.getAppName();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected String getResourceGroup() {
        try {
            return super.getResourceGroup();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected OperatingSystemEnum getOs() {
        try {
            return super.getOs();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected Region getRegion() {
        try {
            return super.getRegion();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected RuntimeStack getRuntimeStack() {
        try {
            return super.getRuntimeStack();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected String getImage() {
        try {
            return super.getImage();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected WebContainer getWebContainer() {
        try {
            return super.getWebContainer();
        } catch (MojoExecutionException e) {
            return null;
        }
    }

    @Override
    protected JavaVersion getJavaVersion() {
        try {
            return super.getJavaVersion();
        } catch (MojoExecutionException e) {
            return null;
        }
    }
}
