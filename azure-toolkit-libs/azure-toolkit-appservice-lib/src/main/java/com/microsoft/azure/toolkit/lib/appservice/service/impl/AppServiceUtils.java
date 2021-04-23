/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.fluent.models.SiteLogsConfigInner;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.ApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.FileSystemApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.FileSystemHttpLogsConfig;
import com.azure.resourcemanager.appservice.models.HttpLogsConfig;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.SkuDescription;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.appservice.models.WebAppDiagnosticLogs;
import com.azure.resourcemanager.resources.fluentcore.model.HasInnerModel;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

class AppServiceUtils {

    static Runtime getRuntimeFromWebApp(WebAppBase webAppBase) {
        if (StringUtils.startsWithIgnoreCase(webAppBase.linuxFxVersion(), "docker")) {
            return Runtime.DOCKER;
        }
        return webAppBase.operatingSystem() == com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS ?
            getRuntimeFromWindowsWebApp(webAppBase) : getRuntimeFromLinuxWebApp(webAppBase);
    }

    private static Runtime getRuntimeFromLinuxWebApp(WebAppBase webAppBase) {
        if (StringUtils.isEmpty(webAppBase.linuxFxVersion())) {
            return Runtime.getRuntime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, JavaVersion.OFF);
        }
        final String linuxFxVersion = webAppBase.linuxFxVersion().replace("|", " ");
        return Runtime.getRuntimeFromLinuxFxVersion(linuxFxVersion);
    }

    private static Runtime getRuntimeFromWindowsWebApp(WebAppBase webAppBase) {
        if (webAppBase.javaVersion() == null || StringUtils.isAnyEmpty(webAppBase.javaContainer(), webAppBase.javaContainerVersion())) {
            return Runtime.getRuntime(OperatingSystem.WINDOWS, WebContainer.JAVA_OFF, JavaVersion.OFF);
        }
        final JavaVersion javaVersion = JavaVersion.values().stream()
            .filter(version -> StringUtils.equals(webAppBase.javaVersion().toString(), version.getValue()))
            .findFirst().orElse(JavaVersion.OFF);
        final String javaContainer = String.join(" ", webAppBase.javaContainer(), webAppBase.javaContainerVersion());
        final WebContainer webContainer = StringUtils.equalsIgnoreCase(webAppBase.javaContainer(), "java") ? WebContainer.JAVA_SE :
            WebContainer.values().stream()
                .filter(container -> StringUtils.equalsIgnoreCase(javaContainer, container.getValue()))
                .findFirst().orElse(WebContainer.JAVA_OFF);
        return Runtime.getRuntime(OperatingSystem.WINDOWS, webContainer, javaVersion);
    }

    static RuntimeStack toLinuxRuntimeStack(Runtime runtime) {
        return RuntimeStack.getAll().stream().filter(runtimeStack -> {
            final Runtime stackRuntime = Runtime.getRuntimeFromLinuxFxVersion(runtimeStack.toString());
            return stackRuntime != null && Objects.equals(stackRuntime.getJavaVersion(), runtime.getJavaVersion()) &&
                Objects.equals(stackRuntime.getWebContainer(), runtime.getWebContainer());
        }).findFirst().orElse(null);
    }

    static com.azure.resourcemanager.appservice.models.WebContainer toWindowsWebContainer(Runtime runtime) {
        if (runtime.getWebContainer() == WebContainer.JAVA_SE) {
            return StringUtils.startsWith(runtime.getJavaVersion().getValue(), JavaVersion.JAVA_8.getValue()) ?
                com.azure.resourcemanager.appservice.models.WebContainer.JAVA_8 :
                com.azure.resourcemanager.appservice.models.WebContainer.fromString("java 11");
        }
        return com.azure.resourcemanager.appservice.models.WebContainer.values().stream()
            .filter(container -> StringUtils.equalsIgnoreCase(container.toString(), runtime.getWebContainer().getValue()))
            .findFirst().orElse(null);
    }

    static com.azure.resourcemanager.appservice.models.JavaVersion toWindowsJavaVersion(Runtime runtime) {
        return com.azure.resourcemanager.appservice.models.JavaVersion.values().stream()
            .filter(serviceVersion -> StringUtils.equalsIgnoreCase(serviceVersion.toString(), runtime.getJavaVersion().getValue()))
            .findFirst().orElse(null);
    }

    static PublishingProfile fromPublishingProfile(com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile) {
        return PublishingProfile.builder()
            .ftpUrl(publishingProfile.ftpUrl())
            .ftpUsername(publishingProfile.ftpUsername())
            .ftpPassword(publishingProfile.ftpPassword())
            .gitUrl(publishingProfile.gitUrl())
            .gitUsername(publishingProfile.gitUsername())
            .gitPassword(publishingProfile.gitPassword()).build();
    }

    static com.azure.resourcemanager.appservice.models.PricingTier toPricingTier(PricingTier pricingTier) {
        final SkuDescription skuDescription = new SkuDescription().withTier(pricingTier.getTier()).withSize(pricingTier.getSize());
        return com.azure.resourcemanager.appservice.models.PricingTier.fromSkuDescription(skuDescription);
    }

    static PricingTier fromPricingTier(com.azure.resourcemanager.appservice.models.PricingTier pricingTier) {
        return PricingTier.values().stream()
            .filter(value -> StringUtils.equals(value.getSize(), pricingTier.toSkuDescription().size()) &&
                StringUtils.equals(value.getTier(), pricingTier.toSkuDescription().tier()))
            .findFirst().orElse(null);
    }

    static OperatingSystem fromOperatingSystem(com.azure.resourcemanager.appservice.models.OperatingSystem operatingSystem) {
        return Arrays.stream(OperatingSystem.values())
            .filter(os -> StringUtils.equals(operatingSystem.name(), os.getValue()))
            .findFirst().orElse(null);
    }

    static JavaVersion fromJavaVersion(com.azure.resourcemanager.appservice.models.JavaVersion javaVersion) {
        return JavaVersion.values().stream()
            .filter(value -> StringUtils.equals(value.getValue(), javaVersion.toString()))
            .findFirst().orElse(null);
    }

    static com.azure.resourcemanager.appservice.models.JavaVersion toJavaVersion(JavaVersion javaVersion) {
        return com.azure.resourcemanager.appservice.models.JavaVersion.values().stream()
            .filter(value -> StringUtils.equals(value.toString(), javaVersion.getValue()))
            .findFirst().orElse(null);
    }

    static WebAppEntity fromWebApp(WebAppBase webAppBase) {
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

    static WebAppEntity fromWebAppBasic(WebAppBasic webAppBasic) {
        return WebAppEntity.builder().name(webAppBasic.name())
            .id(webAppBasic.id())
            .region(Region.fromName(webAppBasic.regionName()))
            .resourceGroup(webAppBasic.resourceGroupName())
            .subscriptionId(Utils.getSubscriptionId(webAppBasic.id()))
            .appServicePlanId(webAppBasic.appServicePlanId())
            .defaultHostName(webAppBasic.defaultHostname())
            .build();
    }

    static WebAppDeploymentSlotEntity fromWebAppDeploymentSlot(DeploymentSlot deploymentSlot) {
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

    static AppServicePlanEntity fromAppServicePlan(com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan) {
        return AppServicePlanEntity.builder()
            .id(appServicePlan.id())
            .subscriptionId(Utils.getSubscriptionId(appServicePlan.id()))
            .name(appServicePlan.name())
            .region(appServicePlan.regionName())
            .resourceGroup(appServicePlan.resourceGroupName())
            .pricingTier(fromPricingTier(appServicePlan.pricingTier()))
            .operatingSystem(fromOperatingSystem(appServicePlan.operatingSystem()))
            .build();
    }

    static AppServicePlan getAppServicePlan(AppServicePlanEntity entity, AzureResourceManager azureClient) {
        try {
            return StringUtils.isNotEmpty(entity.getId()) ?
                azureClient.appServicePlans().getById(entity.getId()) :
                azureClient.appServicePlans().getByResourceGroup(entity.getResourceGroup(), entity.getName());
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            return null;
        }
    }


    static DiagnosticConfig fromWebAppDiagnosticLogs(WebAppDiagnosticLogs webAppDiagnosticLogs) {
        final DiagnosticConfig.DiagnosticConfigBuilder builder = DiagnosticConfig.builder();
        final com.azure.resourcemanager.appservice.models.LogLevel applicationLogLevel = Optional.ofNullable(webAppDiagnosticLogs)
                .map(HasInnerModel::innerModel)
                .map(SiteLogsConfigInner::applicationLogs)
                .map(ApplicationLogsConfig::fileSystem)
                .map(FileSystemApplicationLogsConfig::level).orElse(null);
        if (applicationLogLevel != null && applicationLogLevel != com.azure.resourcemanager.appservice.models.LogLevel.OFF) {
            builder.enableApplicationLog(true).applicationLogLevel(LogLevel.fromString(applicationLogLevel.toString()));
        } else {
            builder.enableApplicationLog(false);
        }
        final FileSystemHttpLogsConfig httpLogsConfig = Optional.ofNullable(webAppDiagnosticLogs)
                .map(HasInnerModel::innerModel)
                .map(SiteLogsConfigInner::httpLogs)
                .map(HttpLogsConfig::fileSystem).orElse(null);
        if (httpLogsConfig != null && httpLogsConfig.enabled()) {
            builder.enableWebServerLogging(true).webServerLogQuota(httpLogsConfig.retentionInMb())
                    .webServerRetentionPeriod(httpLogsConfig.retentionInDays())
                    .enableDetailedErrorMessage(webAppDiagnosticLogs.detailedErrorMessages())
                    .enableFailedRequestTracing(webAppDiagnosticLogs.failedRequestsTracing());
        } else {
            builder.enableWebServerLogging(false);
        }
        return builder.build();
    }

    static void defineDiagnosticConfigurationForWebAppBase(final WebAppBase.DefinitionStages.WithCreate withCreate, final DiagnosticConfig diagnosticConfig) {
        if (diagnosticConfig.isEnableApplicationLog()) {
            withCreate.defineDiagnosticLogsConfiguration()
                    .withApplicationLogging()
                    .withLogLevel(com.azure.resourcemanager.appservice.models.LogLevel.fromString(diagnosticConfig.getApplicationLogLevel().getValue()))
                    .withApplicationLogsStoredOnFileSystem().attach();
        }
        if (diagnosticConfig.isEnableWebServerLogging()) {
            withCreate.defineDiagnosticLogsConfiguration().withWebServerLogging()
                    .withWebServerLogsStoredOnFileSystem()
                    .withWebServerFileSystemQuotaInMB(diagnosticConfig.getWebServerLogQuota())
                    .withLogRetentionDays(diagnosticConfig.getWebServerRetentionPeriod())
                    .withDetailedErrorMessages(diagnosticConfig.isEnableDetailedErrorMessage())
                    .withFailedRequestTracing(diagnosticConfig.isEnableFailedRequestTracing()).attach();
        }
    }

    static void updateDiagnosticConfigurationForWebAppBase(final WebAppBase.Update update, final DiagnosticConfig diagnosticConfig) {
        final WebAppDiagnosticLogs.UpdateStages.Blank<WebAppBase.Update> blank = update.updateDiagnosticLogsConfiguration();
        if (diagnosticConfig.isEnableApplicationLog()) {
            blank.withApplicationLogging()
                    .withLogLevel(com.azure.resourcemanager.appservice.models.LogLevel.fromString(diagnosticConfig.getApplicationLogLevel().getValue()))
                    .withApplicationLogsStoredOnFileSystem().parent();
        } else {
            blank.withoutApplicationLogging().parent();
        }
        if (diagnosticConfig.isEnableWebServerLogging()) {
            blank.withWebServerLogging()
                    .withWebServerLogsStoredOnFileSystem()
                    .withWebServerFileSystemQuotaInMB(diagnosticConfig.getWebServerLogQuota())
                    .withLogRetentionDays(diagnosticConfig.getWebServerRetentionPeriod())
                    .withDetailedErrorMessages(diagnosticConfig.isEnableDetailedErrorMessage())
                    .withFailedRequestTracing(diagnosticConfig.isEnableFailedRequestTracing()).parent();
        } else {
            blank.withoutWebServerLogging().parent();
        }
    }
}
