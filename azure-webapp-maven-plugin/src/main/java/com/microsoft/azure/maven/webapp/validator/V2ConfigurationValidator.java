/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.validator;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeSetting;
import com.microsoft.azure.maven.webapp.utils.JavaVersionUtils;
import com.microsoft.azure.maven.webapp.utils.WebContainerUtils;
import com.microsoft.azure.toolkits.appservice.model.JavaVersion;
import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebContainer;
import com.microsoft.azure.tools.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static com.microsoft.azure.maven.webapp.configuration.MavenRuntimeSetting.RUNTIME_CONFIG_REFERENCE;

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
        final MavenRuntimeSetting runtime = mojo.getRuntime();
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
        final MavenRuntimeSetting mavenRuntimeSetting = mojo.getRuntime();
        if (mavenRuntimeSetting == null || mavenRuntimeSetting.isEmpty()) {
            return null;
        }
        if (!StringUtils.equalsIgnoreCase(mavenRuntimeSetting.getOs(), OperatingSystem.LINUX.getValue())) {
            return null;
        }
        final JavaVersion javaVersion = JavaVersionUtils.toLibraryJavaVersion(mavenRuntimeSetting.getJavaVersionRaw());
        final WebContainer webContainer = WebContainerUtils.toLibraryWebContainer(mavenRuntimeSetting.getWebContainerRaw());
        final Runtime result = Runtime.values().stream().filter(runtime -> runtime.getOperatingSystem() == OperatingSystem.LINUX)
                .filter(runtime -> runtime.getJavaVersion() == javaVersion && runtime.getWebContainer() == webContainer)
                .findAny().orElse(null);
        return result == null ? String.format("Unsupported value \"%s - %s\" for linux runtime, please refer %s " +
                "more information", mavenRuntimeSetting.getWebContainerRaw(), mavenRuntimeSetting.getJavaVersionRaw(), RUNTIME_CONFIG_REFERENCE) : null;
    }

    @Override
    public String validateJavaVersion() {
        final MavenRuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please config the <runtime> in pom.xml.";
        }
        final String javaVersionRaw = runtime.getJavaVersionRaw();
        if (JavaVersion.fromString(javaVersionRaw) == null && JavaVersionUtils.parseJavaVersionEnum(javaVersionRaw) == null) {
            return String.format("The configuration <javaVersion> in pom.xml is not correct, please refer %s.", RUNTIME_CONFIG_REFERENCE);
        }
        return null;
    }

    @Override
    public String validateWebContainer() {
        final MavenRuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please config the <runtime> in pom.xml.";
        }
        if (WebContainer.fromString(runtime.getWebContainerRaw()) == null && !WebContainerUtils.isJavaSeWebContainer(runtime.getWebContainerRaw())) {
            return String.format("The configuration <webContainer> in pom.xml is not correct, please refer %s.", RUNTIME_CONFIG_REFERENCE);
        }
        return null;
    }

    @Override
    public String validateImage() {
        final MavenRuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please configure the <runtime> in pom.xml.";
        }
        if (StringUtils.isEmpty(runtime.getImage())) {
            return "Please config the <image> of <runtime> in pom.xml.";
        }
        return null;
    }
}
