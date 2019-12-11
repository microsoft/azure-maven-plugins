/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.validator;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import static com.microsoft.azure.maven.webapp.configuration.RuntimeSetting.RUNTIME_CONFIG_REFERENCE;

public class V2ConfigurationValidator extends AbstractConfigurationValidator {

    public static final String[] VALID_OS = new String[]{"windows", "linux", "docker"};

    public V2ConfigurationValidator(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public String validateRegion() {
        final String region = mojo.getRegion();
        if (!StringUtils.isEmpty(region) && Region.findByLabelOrName(region) == null) {
            return "The value of <region> is not supported, please correct it in pom.xml.";
        }
        return null;
    }

    @Override
    public String validateOs() {
        final RuntimeSetting runtime = mojo.getRuntime();
        final String os = runtime.getOs();
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
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null || runtime.isEmpty()) {
            return null;
        }
        final RuntimeStack result = runtime.getLinuxRuntime();
        return result == null ? String.format("Unsupported values for linux runtime, please refer %s " +
                "more information", RUNTIME_CONFIG_REFERENCE) : null;
    }

    @Override
    public String validateJavaVersion() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Pleas config the <runtime> in pom.xml.";
        }
        if (runtime.getJavaVersion() == null || !JavaVersion.values().contains(runtime.getJavaVersion())) {
            return "The configuration <javaVersion> in pom.xml is not correct.";
        }
        return null;
    }

    @Override
    public String validateWebContainer() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Pleas config the <runtime> in pom.xml.";
        }
        if (runtime.getWebContainer() == null) {
            return "The configuration <webContainer> in pom.xml is not correct.";
        }
        return null;
    }

    public String validateImage() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return "Please configure the <runtime> in pom.xml.";
        }
        if (StringUtils.isEmpty(runtime.getImage())) {
            return "Please config the <image> of <runtime> in pom.xml.";
        }
        return null;
    }
}
