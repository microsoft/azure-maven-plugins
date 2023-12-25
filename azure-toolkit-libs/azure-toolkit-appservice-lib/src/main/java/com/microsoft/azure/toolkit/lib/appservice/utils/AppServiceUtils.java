/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.utils;

import com.azure.resourcemanager.appservice.fluent.models.SiteLogsConfigInner;
import com.azure.resourcemanager.appservice.models.ApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.FileSystemApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.FileSystemHttpLogsConfig;
import com.azure.resourcemanager.appservice.models.FunctionEnvelope;
import com.azure.resourcemanager.appservice.models.HttpLogsConfig;
import com.azure.resourcemanager.appservice.models.SkuDescription;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppDiagnosticLogs;
import com.azure.resourcemanager.resources.fluentcore.model.HasInnerModel;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.CsmDeploymentStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions;
import com.microsoft.azure.toolkit.lib.appservice.model.DeploymentBuildStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ErrorEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.KuduDeploymentResult;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AppServiceUtils {
    private static final String SCRIPT_FILE = "scriptFile";
    private static final String ENTRY_POINT = "entryPoint";
    private static final String BINDINGS = "bindings";

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
        //noinspection unchecked
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
