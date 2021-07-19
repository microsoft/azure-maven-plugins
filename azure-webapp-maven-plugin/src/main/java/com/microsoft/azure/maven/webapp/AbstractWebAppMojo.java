/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.utils.SystemPropertyUtils;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.maven.webapp.parser.ConfigParser;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.validator.SchemaValidator;
import com.microsoft.azure.toolkit.lib.common.validator.ValidationMessage;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DockerImageType;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base abstract class for Web App Mojos.
 */
public abstract class AbstractWebAppMojo extends AbstractAppServiceMojo {
    public static final String JAVA_VERSION_KEY = "javaVersion";
    public static final String JAVA_WEB_CONTAINER_KEY = "javaWebContainer";
    public static final String DOCKER_IMAGE_TYPE_KEY = "dockerImageType";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
    public static final String OS_KEY = "os";
    public static final String INVALID_CONFIG_KEY = "invalidConfiguration";
    public static final String SCHEMA_VERSION_KEY = "schemaVersion";
    public static final String DEPLOY_TO_SLOT_KEY = "isDeployToSlot";

    //region Properties

    /**
     * App Service pricing tier, which will only be used to create Web App at the first time.<p>
     * Below is the list of supported pricing tier:
     * <ul>
     *     <li>F1</li>
     *     <li>D1</li>
     *     <li>B1</li>
     *     <li>B2</li>
     *     <li>B3</li>
     *     <li>S1</li>
     *     <li>S2</li>
     *     <li>S3</li>
     *     <li>P1V2</li>
     *     <li>P2V2</li>
     *     <li>P3V2</li>
     * </ul>
     */
    @JsonProperty
    @Parameter(property = "webapp.pricingTier")
    protected String pricingTier;

    /**
     * Flag to control whether stop Web App during deployment.
     */
    @Getter
    @JsonProperty
    @Parameter(property = "webapp.stopAppDuringDeployment", defaultValue = "false")
    protected boolean stopAppDuringDeployment;

    /**
     * Skip execution.
     *
     * @since 0.1.4
     */
    @JsonProperty
    @Parameter(property = "webapp.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * App Service region, which will only be used to create App Service at the first time.
     */
    @JsonProperty
    @Parameter(property = "webapp.region")
    protected String region;

    /**
     * Schema version, which will be used to indicate the version of settings schema to use.
     *
     * @since 2.0.0
     */
    @JsonProperty
    @Parameter(property = "schemaVersion", defaultValue = "v2")
    protected String schemaVersion;

    /**
     * Runtime setting
     *
     * @since 2.0.0
     */
    @JsonProperty
    @Parameter(property = "runtime")
    protected MavenRuntimeConfig runtime;

    /**
     * Deployment setting
     *
     * @since 2.0.0
     */
    @JsonProperty
    @Parameter(property = "deployment")
    protected Deployment deployment;

    @JsonIgnore
    private WebAppConfiguration webAppConfiguration;

    @JsonIgnore
    protected File stagingDirectory;

    @JsonIgnore
    protected AzureAppService az;

    @JsonIgnore
    private boolean isRuntimeInjected = false;

    @JsonIgnore
    private WebAppConfig config;

    //endregion

    //region Getter

    @Override
    protected boolean isSkipMojo() {
        return skip;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    @Override
    public String getAppName() {
        return appName == null ? "" : appName;
    }

    @Override
    public String getAppServicePlanResourceGroup() {
        return appServicePlanResourceGroup;
    }

    @Override
    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    public String getRegion() {
        return region;
    }

    public String getPricingTier() {
        return this.pricingTier;
    }

    public boolean isDeployToDeploymentSlot() {
        return getDeploymentSlotSetting() != null;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public MavenRuntimeConfig getRuntime() {
        if (!isRuntimeInjected) {
            setRuntime((MavenRuntimeConfig) SystemPropertyUtils.injectCommandLineParameter("runtime", runtime, MavenRuntimeConfig.class));
            isRuntimeInjected = true;
        }
        return runtime;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setRuntime(final MavenRuntimeConfig runtime) {
        this.runtime = runtime;
    }
    //endregion

    //region Telemetry Configuration Interface

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = super.getTelemetryProperties();
        final MavenRuntimeConfig runtimeConfig = getRuntime();
        final String os = Optional.ofNullable(runtimeConfig).map(MavenRuntimeConfig::getOs).orElse(StringUtils.EMPTY);
        map.put(SCHEMA_VERSION_KEY, schemaVersion);
        map.put(OS_KEY, os);
        if (StringUtils.equalsIgnoreCase(os, OperatingSystem.DOCKER.getValue())) {
            final String imageType = AppServiceUtils.getDockerImageType(runtimeConfig.getImage(), StringUtils.isEmpty(runtimeConfig.getServerId()),
                    runtimeConfig.getRegistryUrl()).name();
            map.put(DOCKER_IMAGE_TYPE_KEY, imageType);
        } else {
            map.put(DOCKER_IMAGE_TYPE_KEY, DockerImageType.NONE.toString());
        }
        map.put(JAVA_VERSION_KEY, Optional.ofNullable(runtimeConfig).map(MavenRuntimeConfig::getJavaVersion).orElse(StringUtils.EMPTY));
        map.put(JAVA_WEB_CONTAINER_KEY, Optional.ofNullable(runtimeConfig).map(MavenRuntimeConfig::getWebContainer).orElse(StringUtils.EMPTY));
        try {
            map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType().toString());
        } catch (AzureExecutionException e) {
            map.put(DEPLOYMENT_TYPE_KEY, "Unknown deployment type.");
        }
        final boolean isDeployToSlot = Optional.ofNullable(getDeploymentSlotSetting()).map(DeploymentSlotSetting::getName)
                .map(StringUtils::isNotEmpty).orElse(false);
        map.put(DEPLOY_TO_SLOT_KEY, String.valueOf(isDeployToSlot));
        return map;
    }

    protected void validateConfiguration(Consumer<ValidationMessage> validationMessageConsumer, boolean failOnError) {
        final Set<ValidationMessage> validate = SchemaValidator.getInstance().validate("WebAppConfiguration", this, "configuration");
        validate.forEach(message -> validationMessageConsumer.accept(message));
        if (CollectionUtils.isNotEmpty(validate) && failOnError) {
            throw new AzureToolkitRuntimeException("Invalid values found in configuration, please correct the value with messages above");
        }
    }

    protected synchronized WebAppConfig getWebAppConfig() throws AzureExecutionException {
        if (config == null) {
            config = new ConfigParser(this).parse();
        }
        return config;
    }

    @Override
    public String getSubscriptionId() {
        return appServiceClient == null ? this.subscriptionId : appServiceClient.getDefaultSubscription().getId();
    }

    @Override
    public String getDeploymentStagingDirectoryPath() {
        if (stagingDirectory == null) {
            synchronized (this) {
                if (stagingDirectory == null) {
                    final String outputFolder = this.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
                    final String stagingDirectoryPath = Paths.get(this.getBuildDirectoryAbsolutePath(),
                            outputFolder, String.format("%s-%s", this.getAppName(), UUID.randomUUID().toString())
                    ).toString();
                    stagingDirectory = new File(stagingDirectoryPath);
                    // If staging directory doesn't exist, create one and delete it on exit
                    if (!stagingDirectory.exists()) {
                        stagingDirectory.mkdirs();
                    }
                }
            }
        }
        return stagingDirectory.getPath();
    }
    //endregion
}
