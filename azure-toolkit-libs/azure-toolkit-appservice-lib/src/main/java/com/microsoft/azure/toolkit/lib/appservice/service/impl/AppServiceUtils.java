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
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionAppBasic;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.FunctionEnvelope;
import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack;
import com.azure.resourcemanager.appservice.models.HttpLogsConfig;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.SkuDescription;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.appservice.models.WebAppDiagnosticLogs;
import com.azure.resourcemanager.resources.fluentcore.model.HasInnerModel;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
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
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class AppServiceUtils {
    private static final String SCRIPT_FILE = "scriptFile";
    private static final String ENTRY_POINT = "entryPoint";
    private static final String BINDINGS = "bindings";

    static Runtime getRuntimeFromAppService(WebAppBase webAppBase) {
        if (StringUtils.startsWithIgnoreCase(webAppBase.linuxFxVersion(), "docker")) {
            return Runtime.DOCKER;
        }
        return webAppBase.operatingSystem() == com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS ?
            getRuntimeFromWindowsAppService(webAppBase) : getRuntimeFromLinuxAppService(webAppBase);
    }

    private static Runtime getRuntimeFromLinuxAppService(WebAppBase webAppBase) {
        if (StringUtils.isEmpty(webAppBase.linuxFxVersion())) {
            return Runtime.getRuntime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, JavaVersion.OFF);
        }
        final String linuxFxVersion = webAppBase.linuxFxVersion();
        return StringUtils.containsIgnoreCase(webAppBase.innerModel().kind(), "function") ?
                getRuntimeFromLinuxFunctionApp(linuxFxVersion) :  Runtime.getRuntimeFromLinuxFxVersion(linuxFxVersion);
    }

    private static Runtime getRuntimeFromLinuxFunctionApp(String linuxFxVersion) {
        final JavaVersion javaVersion = JavaVersion.fromString(linuxFxVersion.replace("|", " "));
        return Runtime.getRuntime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, javaVersion);
    }

    private static Runtime getRuntimeFromWindowsAppService(WebAppBase webAppBase) {
        final String javaContainer = String.join(" ", webAppBase.javaContainer(), webAppBase.javaContainerVersion());
        final WebContainer webContainer = WebContainer.fromString(javaContainer);
        final JavaVersion javaVersion = JavaVersion.fromString(Objects.toString(webAppBase.javaVersion(), null));
        return Runtime.getRuntime(OperatingSystem.WINDOWS, webContainer, javaVersion);
    }

    static RuntimeStack toRuntimeStack(Runtime runtime) {
        return RuntimeStack.getAll().stream().filter(runtimeStack -> {
            final String linuxFxVersion = String.format("%s|%s", runtimeStack.stack(), runtimeStack.version());
            final Runtime stackRuntime = Runtime.getRuntimeFromLinuxFxVersion(linuxFxVersion);
            return Objects.equals(stackRuntime.getJavaVersion(), runtime.getJavaVersion()) &&
                    Objects.equals(stackRuntime.getWebContainer(), runtime.getWebContainer());
        }).findFirst().orElseGet(() -> {
            if (Objects.equals(runtime.getWebContainer(), WebContainer.JAVA_SE)) {
                return new RuntimeStack("JAVA", getJavaVersionValueForJavaSERuntimeStack(runtime.getJavaVersion()));
            }
            final String[] containerInfo = runtime.getWebContainer().getValue().split(" ");
            final String stack = containerInfo[0];
            final String stackVersion = containerInfo[1];
            final String javaVersion = getJavaVersionValueForContainerRuntimeStack(runtime.getJavaVersion());
            final String version = String.format("%s-%s", stackVersion, javaVersion);
            return new RuntimeStack(stack, version);
        });
    }

    static String getJavaVersionValueForJavaSERuntimeStack(@Nonnull JavaVersion javaVersion) {
        // Java SE with minor version runtime stack follow pattern JAVA|VERSION, like JAVA|11.0.9, JAVA|8u25
        return javaVersion.getValue().replaceAll("java|jre", "").trim();
    }

    static String getJavaVersionValueForContainerRuntimeStack(@Nonnull JavaVersion javaVersion) {
        // Runtime stack for java containers follow pattern STACK|STACK_VERSION-JAVA_VERSION, like TOMCAT|9.0.41-java11, JBOSSEAP|7-java8
        if (Objects.equals(javaVersion, JavaVersion.JAVA_8)) {
            return "java8";
        }
        if (Objects.equals(javaVersion, JavaVersion.JAVA_8)) {
            return "java11";
        }
        if (StringUtils.startsWithAny(javaVersion.getValue().toLowerCase(), "java", "jre")) {
            return javaVersion.getValue();
        }
        return String.format("java%s", javaVersion.getValue());
    }

    static FunctionRuntimeStack toFunctionRuntimeStack(@Nonnull Runtime runtime) {
        if (runtime.getOperatingSystem() != OperatingSystem.LINUX) {
            throw new AzureToolkitRuntimeException(String.format("Can not convert %s runtime to FunctionRuntimeStack", runtime.getOperatingSystem()));
        }
        if (Objects.equals(runtime.getJavaVersion(), JavaVersion.JAVA_8)) {
            return FunctionRuntimeStack.JAVA_8;
        }
        if (Objects.equals(runtime.getJavaVersion(), JavaVersion.JAVA_11)) {
            return FunctionRuntimeStack.JAVA_11;
        }
        final String javaVersion = String.format("java|%s", runtime.getJavaVersion().getValue());
        return new FunctionRuntimeStack("java", "~3", javaVersion);
    }

    static com.azure.resourcemanager.appservice.models.WebContainer toWebContainer(Runtime runtime) {
        final WebContainer webContainer = runtime.getWebContainer();
        if (webContainer == null || Objects.equals(webContainer, WebContainer.JAVA_OFF)) {
            return null;
        }
        if (Objects.equals(webContainer, WebContainer.JAVA_SE)) {
            return StringUtils.startsWith(runtime.getJavaVersion().getValue(), JavaVersion.JAVA_8.getValue()) ?
                    com.azure.resourcemanager.appservice.models.WebContainer.JAVA_8 :
                    com.azure.resourcemanager.appservice.models.WebContainer.fromString("java 11");
        }
        return com.azure.resourcemanager.appservice.models.WebContainer.fromString(webContainer.getValue());
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

    static PricingTier fromPricingTier(@Nonnull com.azure.resourcemanager.appservice.models.PricingTier pricingTier) {
        return PricingTier.fromString(pricingTier.toSkuDescription().tier(), pricingTier.toSkuDescription().size());
    }

    static OperatingSystem fromOperatingSystem(com.azure.resourcemanager.appservice.models.OperatingSystem operatingSystem) {
        return OperatingSystem.fromString(operatingSystem.name());
    }

    static com.azure.resourcemanager.appservice.models.JavaVersion toJavaVersion(JavaVersion javaVersion) {
        // remove the java/jre prefix for user input
        final String value = javaVersion.getValue().replaceFirst("(?i)java|jre", "");
        return com.azure.resourcemanager.appservice.models.JavaVersion.fromString(value);
    }

    static FunctionAppEntity fromFunctionApp(FunctionApp functionApp) {
        return FunctionAppEntity.builder().name(functionApp.name())
                .id(functionApp.id())
                .region(Region.fromName(functionApp.regionName()))
                .resourceGroup(functionApp.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(functionApp.id()))
                .runtime(getRuntimeFromAppService(functionApp))
                .appServicePlanId(functionApp.appServicePlanId())
                .defaultHostName(functionApp.defaultHostname())
                .appSettings(Utils.normalizeAppSettings(functionApp.getAppSettings()))
                .build();
    }

    static FunctionAppEntity fromFunctionAppBasic(FunctionAppBasic functionApp) {
        return FunctionAppEntity.builder().name(functionApp.name())
                .id(functionApp.id())
                .region(Region.fromName(functionApp.regionName()))
                .resourceGroup(functionApp.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(functionApp.id()))
                .runtime(null)
                .appServicePlanId(functionApp.appServicePlanId())
                .defaultHostName(functionApp.defaultHostname())
                .build();
    }

    static WebAppEntity fromWebApp(WebAppBase webAppBase) {
        final WebAppEntity.WebAppEntityBuilder<?, ?> builder = WebAppEntity.builder().name(webAppBase.name())
            .id(webAppBase.id())
            .region(Region.fromName(webAppBase.regionName()))
            .resourceGroup(webAppBase.resourceGroupName())
            .subscriptionId(Utils.getSubscriptionId(webAppBase.id()))
            .runtime(getRuntimeFromAppService(webAppBase))
            .appServicePlanId(webAppBase.appServicePlanId())
            .defaultHostName(webAppBase.defaultHostname())
            .appSettings(Utils.normalizeAppSettings(webAppBase.getAppSettings()));

        final String linuxFxVersion = webAppBase.linuxFxVersion();
        if (StringUtils.startsWithIgnoreCase(linuxFxVersion, "docker")) {
            builder.dockerImageName(getDockerImageNameFromLinuxFxVersion(linuxFxVersion));
        }
        return builder.build();
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

    static FunctionAppDeploymentSlotEntity fromFunctionAppDeploymentSlot(FunctionDeploymentSlot deploymentSlot) {
        return FunctionAppDeploymentSlotEntity.builder()
                .name(deploymentSlot.name())
                .functionAppName(deploymentSlot.parent().name())
                .id(deploymentSlot.id())
                .resourceGroup(deploymentSlot.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(deploymentSlot.id()))
                .region(Region.fromName(deploymentSlot.regionName()))
                .runtime(getRuntimeFromAppService(deploymentSlot))
                .appServicePlanId(deploymentSlot.appServicePlanId())
                .defaultHostName(deploymentSlot.defaultHostname())
                .appSettings(Utils.normalizeAppSettings(deploymentSlot.getAppSettings()))
                .build();
    }

    static WebAppDeploymentSlotEntity fromWebAppDeploymentSlot(DeploymentSlot deploymentSlot) {
        return WebAppDeploymentSlotEntity.builder()
                .name(deploymentSlot.name())
                .webappName(deploymentSlot.parent().name())
                .id(deploymentSlot.id())
                .resourceGroup(deploymentSlot.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(deploymentSlot.id()))
                .region(Region.fromName(deploymentSlot.regionName()))
                .runtime(getRuntimeFromAppService(deploymentSlot))
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

    static <T extends WebAppBase> void defineDiagnosticConfigurationForWebAppBase(final WebAppBase.DefinitionStages.WithCreate<T> withCreate,
                                                                                  final DiagnosticConfig diagnosticConfig) {
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

    static <T extends WebAppBase> void updateDiagnosticConfigurationForWebAppBase(final WebAppBase.Update<T> update, final DiagnosticConfig diagnosticConfig) {
        final WebAppDiagnosticLogs.UpdateStages.Blank<WebAppBase.Update<T>> blank = update.updateDiagnosticLogsConfiguration();
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

    public static FunctionEntity fromFunctionAppEnvelope(@Nonnull FunctionEnvelope functionEnvelope) {
        final Object config = functionEnvelope.config();
        if (!(config instanceof Map)) {
            return null;
        }
        final Map<?, ?> envelopeConfigMap = (Map<?, ?>) config;
        final String scriptFile = (String) (envelopeConfigMap).get(SCRIPT_FILE);
        final String entryPoint = (String) (envelopeConfigMap).get(ENTRY_POINT);
        final Object bindingListObject = envelopeConfigMap.get(BINDINGS);
        final List<FunctionEntity.BindingEntity> bindingEntities =
                Optional.ofNullable(bindingListObject instanceof List<?> ? (List<?>) bindingListObject : null)
                        .map(list -> list.stream().filter(item -> item instanceof Map)
                                .map(map -> fromJsonBinding((Map<String, String>) map))
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
        return FunctionEntity.builder()
                .name(getFunctionTriggerName(functionEnvelope))
                .entryPoint(entryPoint)
                .scriptFile(scriptFile)
                .bindingList(bindingEntities)
                .functionAppId(functionEnvelope.functionAppId())
                .triggerId(functionEnvelope.innerModel().id())
                .triggerUrl(functionEnvelope.innerModel().invokeUrlTemplate())
                .build();
    }

    private static String getFunctionTriggerName(@Nonnull FunctionEnvelope functionEnvelope) {
        final String fullName = functionEnvelope.innerModel().name();
        final String[] splitNames = fullName.split("/");
        return splitNames.length > 1 ? splitNames[1] : fullName;
    }

    private static FunctionEntity.BindingEntity fromJsonBinding(Map<String, String> bindingProperties) {
        return FunctionEntity.BindingEntity.builder()
                .type(bindingProperties.get("type"))
                .direction(bindingProperties.get("direction"))
                .name(bindingProperties.get("name"))
                .properties(bindingProperties).build();
    }

    private static String getDockerImageNameFromLinuxFxVersion(String linuxFxVersion) {
        String[] segments = linuxFxVersion.split(Pattern.quote("|"));
        if (segments.length != 2) {
            return null;
        }
        final String image = segments[1];
        if (!image.contains("/")) {
            return image;
        }
        segments = image.split(Pattern.quote("/"));
        return segments[segments.length - 1].trim();
    }
}
