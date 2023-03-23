/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.utils;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.fluent.models.SiteLogsConfigInner;
import com.azure.resourcemanager.appservice.models.ApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.FileSystemApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.FileSystemHttpLogsConfig;
import com.azure.resourcemanager.appservice.models.FunctionEnvelope;
import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack;
import com.azure.resourcemanager.appservice.models.HttpLogsConfig;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.SkuDescription;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppDiagnosticLogs;
import com.azure.resourcemanager.resources.fluentcore.model.HasInnerModel;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.model.CsmDeploymentStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions;
import com.microsoft.azure.toolkit.lib.appservice.model.DeploymentBuildStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ErrorEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.KuduDeploymentResult;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.ANONYMOUS;

public class AppServiceUtils {
    private static final String SCRIPT_FILE = "scriptFile";
    private static final String ENTRY_POINT = "entryPoint";
    private static final String BINDINGS = "bindings";

    public static Runtime getRuntimeFromAppService(WebAppBase webAppBase) {
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
                getRuntimeFromLinuxFunctionApp(linuxFxVersion) : Runtime.getRuntimeFromLinuxFxVersion(linuxFxVersion);
    }

    private static Runtime getRuntimeFromLinuxFunctionApp(String linuxFxVersion) {
        final JavaVersion javaVersion = JavaVersion.fromString(linuxFxVersion.replace("|", " "));
        return StringUtils.containsIgnoreCase(linuxFxVersion, "java") ?
                Runtime.getRuntime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, javaVersion) : Runtime.getRuntime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, JavaVersion.OFF);
    }

    private static Runtime getRuntimeFromWindowsAppService(WebAppBase webAppBase) {
        final String javaContainer = String.join(" ", webAppBase.javaContainer(), webAppBase.javaContainerVersion());
        final WebContainer webContainer = StringUtils.isAllEmpty(webAppBase.javaContainer(), webAppBase.javaContainerVersion()) ?
                WebContainer.JAVA_OFF : WebContainer.fromString(javaContainer);
        final JavaVersion javaVersion = Optional.ofNullable(webAppBase.javaVersion())
                .map(version -> JavaVersion.fromString(version.toString()))
                .orElse(JavaVersion.OFF);
        return Runtime.getRuntime(OperatingSystem.WINDOWS, webContainer, javaVersion);
    }

    public static RuntimeStack toRuntimeStack(Runtime runtime) {
        return RuntimeStack.getAll().stream().filter(runtimeStack -> {
            final String linuxFxVersion = String.format("%s|%s", runtimeStack.stack(), runtimeStack.version());
            final Runtime stackRuntime = Runtime.getRuntimeFromLinuxFxVersion(linuxFxVersion);
            return Objects.equals(stackRuntime.getJavaVersion(), runtime.getJavaVersion()) &&
                    Objects.equals(stackRuntime.getWebContainer(), runtime.getWebContainer());
        }).findFirst().orElseGet(() -> {
            if (Objects.equals(runtime.getWebContainer(), WebContainer.JAVA_SE)) {
                return getRuntimeStackForJavaSERuntime(runtime);
            }
            final String[] containerInfo = runtime.getWebContainer().getValue().split(" ");
            if (containerInfo.length != 2) {
                throw new AzureToolkitRuntimeException(String.format("Invalid webContainer '%s'.", runtime.getWebContainer()));
            }
            final String stack = containerInfo[0].toUpperCase(Locale.ENGLISH);
            final String stackVersion = containerInfo[1];
            final String javaVersion = getJavaVersionValueForContainerRuntimeStack(runtime.getJavaVersion());
            final String version = String.format("%s-%s", stackVersion, javaVersion);
            return new RuntimeStack(stack, version);
        });
    }

    static RuntimeStack getRuntimeStackForJavaSERuntime(Runtime runtime) {
        final JavaVersion javaVersion = runtime.getJavaVersion();
        if (Objects.equals(javaVersion, JavaVersion.JAVA_17)) {
            return new RuntimeStack("JAVA", "17-java17");
        }
        return new RuntimeStack("JAVA", getJavaVersionValueForJavaSERuntimeStack(runtime.getJavaVersion()));
    }

    static String getJavaVersionValueForJavaSERuntimeStack(@Nonnull JavaVersion javaVersion) {
        // Java SE with minor version runtime stack follow pattern JAVA|VERSION, like JAVA|11.0.9, JAVA|8u25
        return javaVersion.getValue().replaceAll("(?i)java|jre", "").trim();
    }

    static String getJavaVersionValueForContainerRuntimeStack(@Nonnull JavaVersion javaVersion) {
        // Runtime stack for java containers follow pattern STACK|STACK_VERSION-JAVA_VERSION, like TOMCAT|9.0.41-java11, JBOSSEAP|7-java8
        if (Objects.equals(javaVersion, JavaVersion.JAVA_8)) {
            return "jre8";
        }
        if (Objects.equals(javaVersion, JavaVersion.JAVA_11)) {
            return "java11";
        }
        if (Objects.equals(javaVersion, JavaVersion.JAVA_17)) {
            return "java17";
        }
        if (StringUtils.startsWithAny(javaVersion.getValue().toLowerCase(), "java", "jre")) {
            return javaVersion.getValue();
        }
        return String.format("java%s", javaVersion.getValue());
    }

    public static FunctionRuntimeStack toFunctionRuntimeStack(@Nonnull Runtime runtime, String functionExtensionVersion) {
        if (runtime.getOperatingSystem() != OperatingSystem.LINUX) {
            throw new AzureToolkitRuntimeException(String.format("Can not convert %s runtime to FunctionRuntimeStack", runtime.getOperatingSystem()));
        }
        final String javaVersion;
        if (Objects.equals(runtime.getJavaVersion(), JavaVersion.JAVA_8)) {
            javaVersion = "java|8";
        } else if (Objects.equals(runtime.getJavaVersion(), JavaVersion.JAVA_11)) {
            javaVersion = "java|11";
        } else {
            javaVersion = String.format("java|%s", runtime.getJavaVersion().getValue());
        }
        return new FunctionRuntimeStack("java", functionExtensionVersion, javaVersion);
    }

    public static com.azure.resourcemanager.appservice.models.WebContainer toWebContainer(Runtime runtime) {
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

    public static PublishingProfile fromPublishingProfile(com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile) {
        return PublishingProfile.builder()
            .ftpUrl(publishingProfile.ftpUrl())
            .ftpUsername(publishingProfile.ftpUsername())
            .ftpPassword(publishingProfile.ftpPassword())
            .gitUrl(publishingProfile.gitUrl())
            .gitUsername(publishingProfile.gitUsername())
            .gitPassword(publishingProfile.gitPassword()).build();
    }

    public static com.azure.resourcemanager.appservice.models.PricingTier toPricingTier(PricingTier pricingTier) {
        final SkuDescription skuDescription = new SkuDescription().withTier(pricingTier.getTier()).withSize(pricingTier.getSize());
        return com.azure.resourcemanager.appservice.models.PricingTier.fromSkuDescription(skuDescription);
    }

    static PricingTier fromPricingTier(@Nonnull com.azure.resourcemanager.appservice.models.PricingTier pricingTier) {
        return PricingTier.fromString(pricingTier.toSkuDescription().tier(), pricingTier.toSkuDescription().size());
    }

    static OperatingSystem fromOperatingSystem(com.azure.resourcemanager.appservice.models.OperatingSystem operatingSystem) {
        return OperatingSystem.fromString(operatingSystem.name());
    }

    public static com.azure.resourcemanager.appservice.models.JavaVersion toJavaVersion(JavaVersion javaVersion) {
        // remove the java/jre prefix for user input
        final String value = javaVersion.getValue().replaceFirst("(?i)java|jre", "");
        return com.azure.resourcemanager.appservice.models.JavaVersion.fromString(value);
    }

    public static DiagnosticConfig fromWebAppDiagnosticLogs(WebAppDiagnosticLogs webAppDiagnosticLogs) {
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

    public static <T extends WebAppBase> void defineDiagnosticConfigurationForWebAppBase(final WebAppBase.DefinitionStages.WithCreate<T> withCreate,
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
                    .withWebServerFileSystemQuotaInMB(ObjectUtils.firstNonNull(diagnosticConfig.getWebServerLogQuota(), 0))
                    .withLogRetentionDays(ObjectUtils.firstNonNull(diagnosticConfig.getWebServerRetentionPeriod(), 0))
                    .withDetailedErrorMessages(diagnosticConfig.isEnableDetailedErrorMessage())
                    .withFailedRequestTracing(diagnosticConfig.isEnableFailedRequestTracing()).attach();
        }
    }

    public static <T extends WebAppBase> void updateDiagnosticConfigurationForWebAppBase(final WebAppBase.Update<T> update,
                                                                                         final DiagnosticConfig diagnosticConfig) {
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
                    .withWebServerFileSystemQuotaInMB(ObjectUtils.firstNonNull(diagnosticConfig.getWebServerLogQuota(), 0))
                    .withLogRetentionDays(ObjectUtils.firstNonNull(diagnosticConfig.getWebServerRetentionPeriod(), 0))
                    .withDetailedErrorMessages(diagnosticConfig.isEnableDetailedErrorMessage())
                    .withFailedRequestTracing(diagnosticConfig.isEnableFailedRequestTracing()).parent();
        } else {
            blank.withoutWebServerLogging().parent();
        }
    }

    public static FunctionEntity fromFunctionAppEnvelope(@Nonnull FunctionEnvelope functionEnvelope, @Nonnull String functionId) {
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
                .functionAppId(functionId)
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

    public static com.azure.resourcemanager.appservice.models.DeployOptions toDeployOptions(@Nonnull final DeployOptions deployOptions) {
        return new com.azure.resourcemanager.appservice.models.DeployOptions().withPath(deployOptions.getPath())
                .withCleanDeployment(deployOptions.getCleanDeployment())
                .withRestartSite(deployOptions.getRestartSite())
                .withTrackDeployment(deployOptions.getTrackDeployment());
    }

    public static KuduDeploymentResult fromKuduDeploymentResult(@Nonnull final com.azure.resourcemanager.appservice.models.KuduDeploymentResult result) {
        return KuduDeploymentResult.builder().deploymentId(result.deploymentId()).build();
    }

    public static CsmDeploymentStatus fromCsmDeploymentStatus(@Nonnull final com.azure.resourcemanager.appservice.models.CsmDeploymentStatus deploymentStatus) {
        final DeploymentBuildStatus buildStatus = DeploymentBuildStatus.fromString(deploymentStatus.status().toString());
        final List<ErrorEntity> errors = Optional.ofNullable(deploymentStatus.errors())
                .map(list -> list.stream().map(AppServiceUtils::fromErrorEntity).collect(Collectors.toList())).orElse(Collections.emptyList());
        return CsmDeploymentStatus.builder()
                .deploymentId(deploymentStatus.deploymentId())
                .status(buildStatus)
                .errors(errors)
                .numberOfInstancesSuccessful(deploymentStatus.numberOfInstancesSuccessful())
                .numberOfInstancesFailed(deploymentStatus.numberOfInstancesFailed())
                .numberOfInstancesInProgress(deploymentStatus.numberOfInstancesInProgress())
                .failedInstancesLogs(deploymentStatus.failedInstancesLogs())
                .build();
    }

    private static ErrorEntity fromErrorEntity(@Nonnull final com.azure.resourcemanager.appservice.models.ErrorEntity entity) {
        final List<ErrorEntity> details = Optional.ofNullable(entity.details())
                .map(list -> list.stream().map(AppServiceUtils::fromErrorEntity).collect(Collectors.toList())).orElse(Collections.emptyList());
        final List<ErrorEntity> innerErrors = Optional.ofNullable(entity.innerErrors())
                .map(list -> list.stream().map(AppServiceUtils::fromErrorEntity).collect(Collectors.toList())).orElse(Collections.emptyList());
        return ErrorEntity.builder()
                .details(details)
                .innerErrors(innerErrors)
                .code(entity.code())
                .extendedCode(entity.extendedCode())
                .message(entity.message())
                .messageTemplate(entity.messageTemplate())
                .parameters(entity.parameters())
                .target(entity.target())
                .build();
    }
}
