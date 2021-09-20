/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.zeroturnaround.zip.ZipUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure Functions doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {
    private static final String DEPLOY_START = "Starting deployment...";
    private static final String DEPLOY_FINISH = "Deployment done, you may access your resource through %s";
    private static final String APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT = "Contradictory configurations for application insights," +
            " specify 'appInsightsKey' or 'appInsightsInstance' if you want to enable it, and specify " +
            "'disableAppInsights=true' if you want to disable it.";
    private static final String UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS = "Some http trigger urls cannot be displayed " +
            "because they are non-anonymous. To access the non-anonymous triggers, please refer to https://aka.ms/azure-functions-key.";
    private static final String HTTP_TRIGGER_URLS = "HTTP Trigger Urls:";
    private static final String NO_ANONYMOUS_HTTP_TRIGGER = "No anonymous HTTP Triggers found in deployed function app, skip list triggers.";
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";
    private static final String ARTIFACT_INCOMPATIBLE_WARNING = "Your function app artifact compile version {0} may not compatible with java version {1} in " +
            "configuration.";
    private static final String ARTIFACT_INCOMPATIBLE_ERROR = "Your function app artifact compile version {0} is not compatible with java version {1} in " +
            "configuration, please downgrade the project compile version and try again.";
    private static final String RUNNING = "Running";
    private static final String NO_ARTIFACT_FOUNDED = "Failed to find function artifact '%s.jar' in folder '%s', please re-package the project and try again.";
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";
    private static final int LIST_TRIGGERS_MAX_RETRY = 5;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;
    private static final String SYNCING_TRIGGERS = "Syncing triggers and fetching function information";
    private static final String SYNCING_TRIGGERS_WITH_RETRY = "Syncing triggers and fetching function information (Attempt {0}/{1})...";
    private static final String NO_TRIGGERS_FOUNDED = "No triggers found in deployed function app, " +
            "please try recompile the project by `mvn clean package` and deploy again.";
    private static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    private static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9._\\-()]{1,90}";
    private static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    private static final String EMPTY_APP_NAME = "Please config the <appName> in pom.xml.";
    private static final String INVALID_APP_NAME = "The <appName> only allow alphanumeric characters, hyphens and cannot start or end in a hyphen.";
    private static final String EMPTY_RESOURCE_GROUP = "Please config the <resourceGroup> in pom.xml.";
    private static final String INVALID_RESOURCE_GROUP_NAME = "The <resourceGroup> only allow alphanumeric characters, periods, underscores, " +
            "hyphens and parenthesis and cannot end in a period.";
    private static final String INVALID_SERVICE_PLAN_NAME = "Invalid value for <appServicePlanName>, it need to match the pattern %s";
    private static final String INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME = "Invalid value for <appServicePlanResourceGroup>, " +
            "it only allow alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";
    private static final String EMPTY_SLOT_NAME = "Please config the <name> of <deploymentSlot> in pom.xml";
    private static final String INVALID_SLOT_NAME = "Invalid value of <name> inside <deploymentSlot> in pom.xml, it needs to match the pattern '%s'";
    private static final String EMPTY_IMAGE_NAME = "Please config the <image> of <runtime> in pom.xml.";
    private static final String INVALID_OS = "The value of <os> is not correct, supported values are: windows, linux and docker.";
    private static final String FAILED_TO_LIST_TRIGGERS = "Deployment succeeded, but failed to list http trigger urls.";
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker app service";
    private static final String EXPANDABLE_PRICING_TIER_WARNING = "'%s' may not be a valid pricing tier, " +
            "please refer to https://aka.ms/maven_function_configuration#supported-pricing-tiers for valid values";
    private static final String EXPANDABLE_REGION_WARNING = "'%s' may not be a valid region, " +
            "please refer to https://aka.ms/maven_function_configuration#supported-regions for valid values";
    private static final String EXPANDABLE_JAVA_VERSION_WARNING = "'%s' may not be a valid java version, recommended values are `Java 8` and `Java 11`";

    @Override
    protected void doExecute() throws AzureExecutionException {
        doValidate();
        getOrCreateAzureAppServiceClient();

        final IFunctionAppBase<?> target = createOrUpdateResource(getParser().parseConfig());
        deployArtifact(target);

        if (target instanceof IFunctionApp) {
            listHTTPTriggerUrls((IFunctionApp) target);
        }
    }

    protected void doValidate() throws AzureExecutionException {
        validateParameters();
        validateArtifactCompileVersion();
        validateApplicationInsightsConfiguration();
    }

    // todo: Extract validator for all maven toolkits
    @Deprecated
    protected void validateParameters() {
        // app name
        if (StringUtils.isBlank(appName)) {
            throw new AzureToolkitRuntimeException(EMPTY_APP_NAME);
        }
        if (appName.startsWith("-") || !appName.matches(APP_NAME_PATTERN)) {
            throw new AzureToolkitRuntimeException(INVALID_APP_NAME);
        }
        // resource group
        if (StringUtils.isBlank(resourceGroup)) {
            throw new AzureToolkitRuntimeException(EMPTY_RESOURCE_GROUP);
        }
        if (resourceGroup.endsWith(".") || !resourceGroup.matches(RESOURCE_GROUP_PATTERN)) {
            throw new AzureToolkitRuntimeException(INVALID_RESOURCE_GROUP_NAME);
        }
        // asp name & resource group
        if (StringUtils.isNotEmpty(appServicePlanName) && !appServicePlanName.matches(APP_SERVICE_PLAN_NAME_PATTERN)) {
            throw new AzureToolkitRuntimeException(String.format(INVALID_SERVICE_PLAN_NAME, APP_SERVICE_PLAN_NAME_PATTERN));
        }
        if (StringUtils.isNotEmpty(appServicePlanResourceGroup) &&
                (appServicePlanResourceGroup.endsWith(".") || !appServicePlanResourceGroup.matches(RESOURCE_GROUP_PATTERN))) {
            throw new AzureToolkitRuntimeException(INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME);
        }
        // slot name
        if (deploymentSlotSetting != null && StringUtils.isEmpty(deploymentSlotSetting.getName())) {
            throw new AzureToolkitRuntimeException(EMPTY_SLOT_NAME);
        }
        if (deploymentSlotSetting != null && !deploymentSlotSetting.getName().matches(SLOT_NAME_PATTERN)) {
            throw new AzureToolkitRuntimeException(String.format(INVALID_SLOT_NAME, SLOT_NAME_PATTERN));
        }
        // region
        if (StringUtils.isNotEmpty(region) && Region.fromName(region).isExpandedValue()) {
            AzureMessager.getMessager().warning(String.format(EXPANDABLE_REGION_WARNING, region));
        }
        // os
        if (StringUtils.isNotEmpty(runtime.getOs()) && OperatingSystem.fromString(runtime.getOs()) == null) {
            throw new AzureToolkitRuntimeException(INVALID_OS);
        }
        // java version
        if (StringUtils.isNotEmpty(runtime.getJavaVersion()) && JavaVersion.fromString(runtime.getJavaVersion()).isExpandedValue()) {
            AzureMessager.getMessager().warning(String.format(EXPANDABLE_JAVA_VERSION_WARNING, runtime.getJavaVersion()));
        }
        // pricing tier
        if (StringUtils.isNotEmpty(pricingTier) && PricingTier.fromString(pricingTier).isExpandedValue()) {
            AzureMessager.getMessager().warning(String.format(EXPANDABLE_PRICING_TIER_WARNING, pricingTier));
        }
        // docker image
        if (OperatingSystem.fromString(runtime.getOs()) == OperatingSystem.DOCKER && StringUtils.isEmpty(runtime.getImage())) {
            throw new AzureToolkitRuntimeException(EMPTY_IMAGE_NAME);
        }
    }

    protected IFunctionAppBase<?> createOrUpdateResource(final FunctionAppConfig config) {
        return new CreateOrUpdateFunctionAppTask(config).execute();
    }

    private void deployArtifact(IFunctionAppBase<? extends AppServiceBaseEntity> target) throws AzureExecutionException {
        if (target.getRuntime().isDocker()) {
            AzureMessager.getMessager().info(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE);
            return;
        }
        AzureMessager.getMessager().info(DEPLOY_START);
        final FunctionDeployType deployType = StringUtils.isEmpty(deploymentType) ? null : FunctionDeployType.fromString(deploymentType);
        // For ftp deploy, we need to upload entire staging directory not the zipped package
        final File file = deployType == FunctionDeployType.FTP ? new File(getDeploymentStagingDirectoryPath()) : packageStagingDirectory();
        final RunnableWithException deployRunnable = deployType == null ? () -> target.deploy(file) : () -> target.deploy(file, deployType);
        executeWithTimeRecorder(deployRunnable, DEPLOY);
        // todo: check function status after deployment
        if (!StringUtils.equalsIgnoreCase(target.state(), RUNNING)) {
            target.start();
        }
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, getResourcePortalUrl(target.id())));
    }

    private File packageStagingDirectory() {
        final File zipFile = new File(getDeploymentStagingDirectoryPath() + ".zip");
        final File stagingDirectory = new File(getDeploymentStagingDirectoryPath());

        ZipUtil.pack(stagingDirectory, zipFile);
        ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
        return zipFile;
    }

    /**
     * List anonymous HTTP Triggers url after deployment
     */
    protected void listHTTPTriggerUrls(IFunctionApp target) {
        try {
            final List<FunctionEntity> triggers = listFunctions(target);
            final List<FunctionEntity> httpFunction = triggers.stream()
                    .filter(function -> function.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(function.getTrigger().getType(), HTTP_TRIGGER))
                    .collect(Collectors.toList());
            final List<FunctionEntity> anonymousTriggers = httpFunction.stream()
                    .filter(bindingResource -> bindingResource.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(bindingResource.getTrigger().getProperty(AUTH_LEVEL), AuthorizationLevel.ANONYMOUS.toString()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
                AzureMessager.getMessager().info(NO_ANONYMOUS_HTTP_TRIGGER);
                return;
            }
            AzureMessager.getMessager().info(HTTP_TRIGGER_URLS);
            anonymousTriggers.forEach(trigger -> AzureMessager.getMessager().info(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())));
            if (anonymousTriggers.size() < httpFunction.size()) {
                AzureMessager.getMessager().info(UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS);
            }
        } catch (RuntimeException e) {
            // show warning instead of exception for list triggers
            AzureMessager.getMessager().warning(FAILED_TO_LIST_TRIGGERS);
        }
    }

    private List<FunctionEntity> listFunctions(final IFunctionApp functionApp) {
        final int[] count = {0};
        return Mono.fromCallable(() -> {
            final AzureString message = count[0]++ == 0 ?
                    AzureString.fromString(SYNCING_TRIGGERS) : AzureString.format(SYNCING_TRIGGERS_WITH_RETRY, count[0], LIST_TRIGGERS_MAX_RETRY);
            AzureMessager.getMessager().info(message);
            return Optional.ofNullable(functionApp.listFunctions(true))
                    .filter(CollectionUtils::isNotEmpty)
                    .orElseThrow(() -> new AzureToolkitRuntimeException(NO_TRIGGERS_FOUNDED));
        }).subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.fixedDelay(LIST_TRIGGERS_MAX_RETRY - 1, Duration.ofSeconds(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS))).block();
    }

    protected void validateArtifactCompileVersion() throws AzureExecutionException {
        final RuntimeConfig runtimeConfig = getParser().getRuntimeConfig();
        if (runtimeConfig.os() == OperatingSystem.DOCKER) {
            return;
        }
        final JavaVersion javaVersion = Optional.ofNullable(runtimeConfig.javaVersion()).orElse(CreateOrUpdateFunctionAppTask.DEFAULT_FUNCTION_JAVA_VERSION);
        final ComparableVersion runtimeVersion = new ComparableVersion(javaVersion.getValue());
        final ComparableVersion artifactVersion = new ComparableVersion(Utils.getArtifactCompileVersion(getArtifactToDeploy()));
        if (runtimeVersion.compareTo(artifactVersion) >= 0) {
            return;
        }
        if (javaVersion.isExpandedValue()) {
            AzureMessager.getMessager().warning(AzureString.format(ARTIFACT_INCOMPATIBLE_WARNING, artifactVersion.toString(), runtimeVersion.toString()));
        } else {
            final String errorMessage = AzureString.format(ARTIFACT_INCOMPATIBLE_ERROR, artifactVersion.toString(), runtimeVersion.toString()).toString();
            throw new AzureExecutionException(errorMessage);
        }
    }

    private File getArtifactToDeploy() throws AzureExecutionException {
        final File stagingFolder = new File(getDeploymentStagingDirectoryPath());
        return Arrays.stream(Optional.ofNullable(stagingFolder.listFiles()).orElse(new File[0]))
                .filter(jar -> StringUtils.equals(FilenameUtils.getBaseName(jar.getName()), this.getFinalName()))
                .findFirst()
                .orElseThrow(() -> new AzureExecutionException(String.format(NO_ARTIFACT_FOUNDED, this.getFinalName(), stagingFolder)));
    }

    private void validateApplicationInsightsConfiguration() throws AzureExecutionException {
        if (isDisableAppInsights() && (StringUtils.isNotEmpty(getAppInsightsKey()) || StringUtils.isNotEmpty(getAppInsightsInstance()))) {
            throw new AzureExecutionException(APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT);
        }
    }
}
