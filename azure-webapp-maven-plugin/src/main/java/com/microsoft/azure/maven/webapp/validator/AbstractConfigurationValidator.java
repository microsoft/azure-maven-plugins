/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.validator;

import com.microsoft.azure.common.appservice.AppServiceUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractConfigurationValidator {
    public static final String SUBSCRIPTION_ID_PATTERN = "[a-fA-F0-9\\-]{30,36}";
    public static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    public static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9\\.\\_\\-\\(\\)]{1,90}";
    public static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";

    protected final AbstractWebAppMojo mojo;

    public AbstractConfigurationValidator(AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public String validateAppName() {
        final String appName = mojo.getAppName();
        if (StringUtils.isEmpty(appName)) {
            return "Please config the <appName> in pom.xml.";
        }
        if (appName.startsWith("-") || !appName.matches(APP_NAME_PATTERN)) {
            return "The <appName> only allow alphanumeric characters, " +
                    "hyphens and cannot start or end in a hyphen.";
        }
        return null;
    }

    public String validateResourceGroup() {
        final String resourceGroupName = mojo.getResourceGroup();
        if (StringUtils.isEmpty(resourceGroupName)) {
            return "Please config the <resourceGroup> in pom.xml.";
        }
        if (resourceGroupName.endsWith(".") || !resourceGroupName.matches(RESOURCE_GROUP_PATTERN)) {
            return "The <resourceGroup> only allow alphanumeric characters, periods, underscores," +
                    " hyphens and parenthesis and cannot end in a period.";
        }
        return null;
    }

    public String validatePricingTier() {
        if (mojo.getPricingTier() != null && AppServiceUtils.getPricingTierFromString(mojo.getPricingTier()) == null) {
            return "Unknown value of the pricingTier.";
        }
        return null;
    }

    public String validateDeploymentSlot() {
        final DeploymentSlotSetting deploymentSlotSetting = mojo.getDeploymentSlotSetting();
        if (deploymentSlotSetting == null) {
            return null;
        }
        if (StringUtils.isEmpty(deploymentSlotSetting.getName())) {
            return "Please config the <name> of <deploymentSlot> in pom.xml";
        }
        if (!deploymentSlotSetting.getName().matches(SLOT_NAME_PATTERN)) {
            return String.format("Invalid value of <name> inside <deploymentSlot> in pom.xml," +
                    " it needs to match the pattern '%s'", SLOT_NAME_PATTERN);
        }
        return null;
    }

    public String validateAppServicePlan() {
        // todo: add validation for app service plan name
        return null;
    }

    public abstract String validateRegion();

    public abstract String validateOs();

    public abstract String validateRuntimeStack();

    public abstract String validateImage();

    public abstract String validateJavaVersion();

    public abstract String validateWebContainer();
}
