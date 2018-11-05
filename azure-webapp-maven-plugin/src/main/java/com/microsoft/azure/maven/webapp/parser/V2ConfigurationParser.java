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
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class V2ConfigurationParser extends ConfigurationParser {
    public V2ConfigurationParser(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    protected OperatingSystemEnum getOs() throws MojoExecutionException {
        final RuntimeSetting runtime = mojo.getRuntime();
        final String os = runtime.getOs();
        if (runtime.isEmpty()) {
            return null;
        } else if (StringUtils.isEmpty(os)) {
            throw new MojoExecutionException("Pleas configure the <os> of <runtime> in pom.xml.");
        }
        switch (os.toLowerCase(Locale.ENGLISH)) {
            case "windows":
                return OperatingSystemEnum.Windows;
            case "linux":
                return OperatingSystemEnum.Linux;
            case "docker":
                return OperatingSystemEnum.Docker;
            default:
                throw new MojoExecutionException("The value of <os> is not correct, supported values are: windows, " +
                    "linux and docker.");
        }
    }

    @Override
    protected Region getRegion() throws MojoExecutionException {
        final String region = mojo.getRegion();
        if (StringUtils.isEmpty(region)) {
            throw new MojoExecutionException("Please config the <region> in pom.xml.");
        }
        if (Arrays.asList(Region.values()).contains(region)) {
            throw new MojoExecutionException("The value of <region> is not supported, please correct it in pom.xml.");
        }
        return Region.fromName(region);
    }

    @Override
    protected RuntimeStack getRuntimeStack() throws MojoExecutionException {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null || runtime.isEmpty()) {
            return null;
        }
        return runtime.getLinuxRuntime();
    }

    @Override
    protected String getImage() throws MojoExecutionException {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            throw new MojoExecutionException("Please configure the <runtime> in pom.xml.");
        }
        if (StringUtils.isEmpty(runtime.getImage())) {
            throw new MojoExecutionException("Please config the <image> of <runtime> in pom.xml.");
        }
        return runtime.getImage();
    }

    @Override
    protected String getServerId() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getServerId();
    }

    @Override
    protected String getRegistryUrl() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getRegistryUrl();
    }

    @Override
    protected WebContainer getWebContainer() throws MojoExecutionException {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            throw new MojoExecutionException("Pleas config the <runtime> in pom.xml.");
        }
        if ("windows".equalsIgnoreCase(runtime.getOs()) && runtime.getWebContainer() == null) {
            throw new MojoExecutionException("The configuration <webContainer> in pom.xml is not correct.");
        }
        return runtime.getWebContainer();
    }

    @Override
    protected JavaVersion getJavaVersion() throws MojoExecutionException {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            throw new MojoExecutionException("Pleas config the <runtime> in pom.xml.");
        }
        if (runtime.getJavaVersion() == null) {
            throw new MojoExecutionException("The configuration <javaVersion> in pom.xml is not correct.");
        }
        return runtime.getJavaVersion();
    }

    @Override
    protected List<Resource> getResources() {
        final Deployment deployment = mojo.getDeployment();
        return deployment == null ? null : deployment.getResources();
    }
}
