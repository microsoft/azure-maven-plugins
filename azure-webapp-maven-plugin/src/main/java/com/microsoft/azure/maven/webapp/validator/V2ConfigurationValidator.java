/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.validator;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig.RUNTIME_CONFIG_REFERENCE;

public class V2ConfigurationValidator extends AbstractConfigurationValidator {

    public static final String[] VALID_OS = new String[]{"windows", "linux", "docker"};

    public V2ConfigurationValidator(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public String validateRegion() {
        final String region = mojo.getRegion();
        if (!StringUtils.isEmpty(region) && Region.fromName(region) == null) {
            return "The value of <region> is not supported, please correct it in pom.xml.";
        }
        return null;
    }

    @Override
    public String validateOs() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        final String os = StringUtils.lowerCase(runtime.getOs());
        if (runtime.isEmpty()) {
            return null;
        } else if (StringUtils.isEmpty(os)) {
            return "Pleas configure the <os> of <runtime> in pom.xml.";
        }
        if (!Arrays.asList(VALID_OS).contains(os)) {
            return "The value of <os> is not correct, supported values are: windows, linux and docker.";
        }
        return null;
    }

    @Override
    public String validateRuntimeStack() {
        final MavenRuntimeConfig mavenRuntimeConfig = mojo.getRuntime();
        if (mavenRuntimeConfig == null || mavenRuntimeConfig.isEmpty()) {
            return null;
        }
        if (!StringUtils.equalsIgnoreCase(mavenRuntimeConfig.getOs(), OperatingSystem.LINUX.getValue())) {
            return null;
        }
        final JavaVersion javaVersion = JavaVersion.fromString(mavenRuntimeConfig.getJavaVersionRaw());
        final WebContainer webContainer = WebContainer.fromString(mavenRuntimeConfig.getWebContainerRaw());
        final Runtime result = Runtime.values().stream().filter(runtime -> runtime.getOperatingSystem() == OperatingSystem.LINUX)
                .filter(runtime -> runtime.getJavaVersion() == javaVersion && runtime.getWebContainer() == webContainer)
                .findAny().orElse(null);
        return result == null ? String.format("Unsupported value \"%s - %s\" for linux runtime, please refer %s " +
                "more information", mavenRuntimeConfig.getWebContainerRaw(), mavenRuntimeConfig.getJavaVersionRaw(), RUNTIME_CONFIG_REFERENCE) : null;
    }

    @Override
    public String validateJavaVersion() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please config the <runtime> in pom.xml.";
        }
        final String javaVersionRaw = runtime.getJavaVersionRaw();
        if (JavaVersion.fromString(javaVersionRaw) == JavaVersion.OFF) {
            return String.format("Unsupported value %s for <javaVersion> in pom.xml, please refer %s.", runtime.getJavaVersionRaw(), RUNTIME_CONFIG_REFERENCE);
        }
        return null;
    }

    @Override
    public String validateWebContainer() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please config the <runtime> in pom.xml.";
        }
        if (WebContainer.fromString(runtime.getWebContainerRaw()) == WebContainer.JAVA_OFF) {
            return String.format("Unsupported value %s for <webContainer> in pom.xml, please refer %s.",
                    runtime.getWebContainerRaw(), RUNTIME_CONFIG_REFERENCE);
        }
        return null;
    }

    @Override
    public String validateImage() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please configure the <runtime> in pom.xml.";
        }
        if (StringUtils.isEmpty(runtime.getImage())) {
            return "Please config the <image> of <runtime> in pom.xml.";
        }
        return null;
    }
}
