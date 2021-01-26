/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.impl;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.SkuDescription;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkits.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkits.appservice.model.JavaVersion;
import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebContainer;
import com.microsoft.azure.toolkits.appservice.utils.Utils;
import com.microsoft.azure.tools.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class ConvertUtils {

    static Runtime getRuntimeFromWebApp(WebAppBase webAppBase) {
        if (StringUtils.startsWithIgnoreCase(webAppBase.linuxFxVersion(), "docker")) {
            return Runtime.DOCKER;
        }
        return webAppBase.operatingSystem() == com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS ?
                getRuntimeFromWindowsWebApp(webAppBase) : getRuntimeFromLinuxWebApp(webAppBase);
    }

    static Runtime getRuntimeFromLinuxWebApp(WebAppBase webAppBase) {
        if (StringUtils.isEmpty(webAppBase.linuxFxVersion())) {
            return null;
        }
        final String linuxFxVersion = webAppBase.linuxFxVersion().replace("|", " ");
        return Runtime.getRuntimeFromLinuxFxVersion(linuxFxVersion);
    }

    static Runtime getRuntimeFromWindowsWebApp(WebAppBase webAppBase) {
        if (webAppBase.javaVersion() == null || StringUtils.isAnyEmpty(webAppBase.javaContainer(), webAppBase.javaContainerVersion())) {
            return null;
        }
        final JavaVersion javaVersion = JavaVersion.values().stream()
                .filter(version -> StringUtils.equals(webAppBase.javaVersion().toString(), version.getValue()))
                .findFirst().orElse(null);
        final String javaContainer = String.join(webAppBase.javaContainer(), " ", webAppBase.javaContainerVersion());
        final WebContainer webContainer = StringUtils.equalsIgnoreCase(webAppBase.javaContainer(), "java") ? WebContainer.JAVA_SE :
                WebContainer.values().stream()
                        .filter(container -> StringUtils.equals(javaContainer, container.getValue()))
                        .findFirst().orElse(null);
        return Runtime.getRuntime(OperatingSystem.WINDOWS, webContainer, javaVersion);
    }

    static RuntimeStack convertRuntimeToRuntimeStack(Runtime runtime) {
        if (runtime.getOperatingSystem() != OperatingSystem.LINUX || runtime.getWebContainer() == null || runtime.getJavaVersion() == null) {
            return null;
        }
        final String fixedLinuxJavaVersion = runtime.getJavaVersion().getValue().startsWith(JavaVersion.JAVA_8.getValue()) ? "8" : "11";
        return RuntimeStack.getAll().stream().filter(runtimeStack -> {
            final String[] versionArray = runtimeStack.version().split("-");
            final String runtimeStackJavaVersion = versionArray[1];
            final String runtimeStackContainer = StringUtils.join(runtimeStack.stack(), " ", versionArray[0]);
            return StringUtils.containsIgnoreCase(runtimeStackJavaVersion, fixedLinuxJavaVersion) &&
                    ((StringUtils.containsIgnoreCase(runtimeStackContainer, "java") && runtime.getWebContainer() == WebContainer.JAVA_SE) ||
                            StringUtils.equalsIgnoreCase(runtimeStackContainer, runtime.getWebContainer().getValue()));
        }).findFirst().orElse(null);
    }

    static com.azure.resourcemanager.appservice.models.WebContainer convertRuntimeToWebContainer(Runtime runtime) {
        if (runtime.getOperatingSystem() != OperatingSystem.WINDOWS || runtime.getWebContainer() == null) {
            return null;
        }
        if (runtime.getWebContainer() == WebContainer.JAVA_SE) {
            return StringUtils.startsWith(runtime.getJavaVersion().getValue(), JavaVersion.JAVA_8.getValue()) ?
                    com.azure.resourcemanager.appservice.models.WebContainer.JAVA_8 :
                    com.azure.resourcemanager.appservice.models.WebContainer.fromString("java 11");
        }
        return com.azure.resourcemanager.appservice.models.WebContainer.values().stream()
                .filter(container -> StringUtils.equalsIgnoreCase(container.toString(), runtime.getWebContainer().getValue()))
                .findFirst().orElse(null);
    }

    static com.azure.resourcemanager.appservice.models.JavaVersion convertToServiceJavaVersionModel(Runtime runtime) {
        if (runtime.getOperatingSystem() != OperatingSystem.WINDOWS || runtime.getJavaVersion() == null) {
            return null;
        }
        return com.azure.resourcemanager.appservice.models.JavaVersion.values().stream()
                .filter(serviceVersion -> StringUtils.equals(serviceVersion.toString(), runtime.getJavaVersion().getValue()))
                .findFirst().orElse(null);
    }

    static PublishingProfile getPublishingProfileFromServiceModel(com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile) {
        return PublishingProfile.builder()
                .ftpUrl(publishingProfile.ftpUrl())
                .ftpUsername(publishingProfile.ftpUsername())
                .ftpPassword(publishingProfile.ftpPassword())
                .gitUrl(publishingProfile.gitUrl())
                .gitUsername(publishingProfile.gitUsername())
                .gitPassword(publishingProfile.gitPassword()).build();
    }

    static com.azure.resourcemanager.appservice.models.PricingTier convertPricingTierToServiceModel(PricingTier pricingTier) {
        final SkuDescription skuDescription = new SkuDescription().withTier(pricingTier.getTier()).withSize(pricingTier.getSize());
        return com.azure.resourcemanager.appservice.models.PricingTier.fromSkuDescription(skuDescription);
    }

    static PricingTier getPricingTierFromServiceModel(com.azure.resourcemanager.appservice.models.PricingTier pricingTier) {
        return PricingTier.values().stream()
                .filter(value -> StringUtils.equals(value.getSize(), pricingTier.toSkuDescription().size()) &&
                        StringUtils.equals(value.getTier(), pricingTier.toSkuDescription().tier()))
                .findFirst().orElse(null);
    }

    static OperatingSystem getOSFromServiceModel(com.azure.resourcemanager.appservice.models.OperatingSystem operatingSystem) {
        return Arrays.stream(OperatingSystem.values())
                .filter(os -> StringUtils.equals(operatingSystem.name(), os.getValue()))
                .findFirst().orElse(null);
    }

    static JavaVersion createJavaVersionFromServiceModel(com.azure.resourcemanager.appservice.models.JavaVersion javaVersion) {
        return JavaVersion.values().stream()
                .filter(value -> StringUtils.equals(value.getValue(), javaVersion.toString()))
                .findFirst().orElse(null);
    }

    static com.azure.resourcemanager.appservice.models.JavaVersion convertJavaVersionToServiceModel(JavaVersion javaVersion) {
        return com.azure.resourcemanager.appservice.models.JavaVersion.values().stream()
                .filter(value -> StringUtils.equals(value.toString(), javaVersion.getValue()))
                .findFirst().orElse(null);
    }

    static WebAppEntity createWebAppEntityFromWebAppBase(WebAppBase webAppBase) {
        return WebAppEntity.builder().name(webAppBase.name())
                .id(webAppBase.id())
                .region(Region.fromName(webAppBase.regionName()))
                .resourceGroup(webAppBase.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(webAppBase.id()))
                .runtime(null)
                .appServicePlanId(webAppBase.appServicePlanId())
                .defaultHostName(webAppBase.defaultHostname())
                .appSettings(Utils.normalizeAppSettings(webAppBase.getAppSettings()))
                .build();
    }

    public static WebAppEntity createWebAppEntityFromWebAppBasic(WebAppBasic webAppBasic) {
        return WebAppEntity.builder().name(webAppBasic.name())
                .id(webAppBasic.id())
                .region(Region.fromName(webAppBasic.regionName()))
                .resourceGroup(webAppBasic.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(webAppBasic.id()))
                .appServicePlanId(webAppBasic.appServicePlanId())
                .defaultHostName(webAppBasic.defaultHostname())
                .build();
    }

    public static WebAppDeploymentSlotEntity createSlotEntityFromServiceModel(DeploymentSlot deploymentSlot) {
        return WebAppDeploymentSlotEntity.builder()
                .name(deploymentSlot.name())
                .webappName(deploymentSlot.parent().name())
                .id(deploymentSlot.id())
                .resourceGroup(deploymentSlot.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(deploymentSlot.id()))
                .runtime(null)
                .appServicePlanId(deploymentSlot.appServicePlanId())
                .defaultHostName(deploymentSlot.defaultHostname())
                .appSettings(Utils.normalizeAppSettings(deploymentSlot.getAppSettings()))
                .build();
    }

    public static AppServicePlanEntity createServicePlanEntityFromServiceModel(com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan) {
        return AppServicePlanEntity.builder()
                .id(appServicePlan.id())
                .name(appServicePlan.name())
                .region(appServicePlan.regionName())
                .resourceGroup(appServicePlan.resourceGroupName())
                .pricingTier(getPricingTierFromServiceModel(appServicePlan.pricingTier()))
                .operatingSystem(getOSFromServiceModel(appServicePlan.operatingSystem()))
                .build();
    }
}
