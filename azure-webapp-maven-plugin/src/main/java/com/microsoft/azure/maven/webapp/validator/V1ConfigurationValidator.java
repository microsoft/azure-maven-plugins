/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.validator;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.toolkits.appservice.model.JavaVersion;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class V1ConfigurationValidator extends AbstractConfigurationValidator {

    private static final String RUNTIME_CONFIG_CONFLICT = "Conflict settings found. <javaVersion>, <linuxRuntime>" +
            "and <containerSettings> should not be set at the same time.";

    public V1ConfigurationValidator(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public String validateRegion() {
        if (StringUtils.isNotEmpty(mojo.getRegion()) &&
                !Arrays.asList(Region.values()).contains(Region.fromName(mojo.getRegion()))) {
            return "The value of <region> is not correct, please correct it in pom.xml.";
        }
        return null;
    }

    @Override
    public String validateOs() {
        final String linuxRuntime = mojo.getLinuxRuntime();
        final String javaVersion = mojo.getJavaVersion();
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final boolean isContainerSettingEmpty = containerSetting == null || containerSetting.isEmpty();
        int osCount = 0;
        if (javaVersion != null) {
            osCount++;
        }
        if (linuxRuntime != null) {
            osCount++;
        }
        if (!isContainerSettingEmpty) {
            osCount++;
        }
        if (osCount > 1) {
            return RUNTIME_CONFIG_CONFLICT;
        }
        return null;
    }

    @Override
    public String validateRuntimeStack() {
        if (mojo.getLinuxRuntime() == null) {
            return "Please configure the <linuxRuntime> in pom.xml.";
        }
        return null;
    }

    @Override
    public String validateImage() {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        if (containerSetting == null) {
            return "Please config the <containerSettings> in pom.xml.";
        }
        if (StringUtils.isEmpty(containerSetting.getImageName())) {
            return "Please config the <imageName> of <containerSettings> in pom.xml.";
        }
        return null;
    }

    @Override
    public String validateJavaVersion() {
        final String javaVersion = mojo.getJavaVersion();
        if (StringUtils.isEmpty(javaVersion)) {
            return "Please config the <javaVersion> in pom.xml.";
        }
        if (JavaVersion.fromString(javaVersion) != null) {
            return null;
        }
        return "The configuration of <javaVersion> in pom.xml is not correct.";
    }

    @Override
    public String validateWebContainer() {
        if (mojo.getJavaWebContainer() == null) {
            return "The configuration of <javaWebContainer> in pom.xml is not correct.";
        }
        return null;
    }
}
