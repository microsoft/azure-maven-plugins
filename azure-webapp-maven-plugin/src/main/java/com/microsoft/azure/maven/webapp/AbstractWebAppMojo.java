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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Base abstract class for Web App Mojos.
 */
public abstract class AbstractWebAppMojo extends AbstractAppServiceMojo {
    public static final String JAVA_VERSION_KEY = "javaVersion";
    public static final String JAVA_WEB_CONTAINER_KEY = "javaWebContainer";
    public static final String DOCKER_IMAGE_TYPE_KEY = "dockerImageType";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
    public static final String OS_KEY = "os";
    public static final String SCHEMA_VERSION_KEY = "schemaVersion";
    public static final String DEPLOY_TO_SLOT_KEY = "isDeployToSlot";
    public static final String SKIP_CREATE_RESOURCE_KEY = "skipCreateResource";
    public static final String INVALID_PARAMETER_ERROR_MESSAGE = "Invalid values found in configuration, please correct the value with messages below:";
    //region Properties

    /**
     * Pricing for web app <p>
     * Supported values : F1, D1, B1, B2, B3, S1, S2, S3, P1V2, P2V2, P3V2, P1V3, P2V3, P3V3
     * @Since 0.1.0
     */
    @JsonProperty
    @Parameter(property = "webapp.pricingTier")
    protected String pricingTier;

    /**
     * Boolean flag to control whether stop web app during deployment.
     * @Since 0.1.0
     */
    @Getter
    @JsonProperty
    @Parameter(property = "webapp.stopAppDuringDeployment", defaultValue = "false")
    protected boolean stopAppDuringDeployment;

    /**
     * Boolean flag to skip the execution of maven plugin for azure webapp
     * @since 0.1.4
     */
    @JsonProperty
    @Parameter(property = "webapp.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Boolean flag to skip the resource creation, will throw exception if target resource does not exist in this case
     * @since 2.2.0
     */
    @JsonIgnore
    @Parameter(property = "azure.resource.create.skip", defaultValue = "false")
    protected boolean skipAzureResourceCreate;

    /**
     * Boolean flag to skip the resource creation, will throw exception if target resource does not exist in this case
     * @since 2.2.0
     */
    @JsonIgnore
    @Parameter(property = "skipCreateAzureResource")
    protected boolean skipCreateAzureResource;

    /**
     * Region for web app
     * Supported values: westus, westus2, eastus, eastus2, northcentralus, southcentralus, westcentralus, canadacentral, canadaeast, brazilsouth, northeurope,
     * westeurope, uksouth, eastasia, southeastasia, japaneast, japanwest, australiaeast, australiasoutheast, centralindia, southindia ...
     */
    @JsonProperty
    @Parameter(property = "webapp.region")
    protected String region;

    /**
     * Schema version, which will be used to indicate the version of settings schema to use.
     * @since 1.4.1
     */
    @JsonProperty
    @Deprecated
    @Parameter(property = "schemaVersion", defaultValue = "v2")
    protected String schemaVersion;

    /**
     * Runtime environment of web app <p>
     * Properties for Windows/Linux web app
     * <ul>
     *     <li> os: Operating system for the web app, default to be Windows. </li>
     *     <li> javaVersion: Java runtime version for the web app, supported values are `Java 8` and `Java 11`. </li>
     *     <li> webContainer: Java web container for the web app, supported values are `Tomcat 8.5`, `Tomcat 9.0`, `Java SE`, `Jbosseap 7`(Linux only). </li>
     * </ul>
     * <pre>
     * {@code
     * <runtime>
     *     <os>windows</os>
     *     <javaVersion>Java 8</javaVersion>
     *     <webContainer>Java SE</webContainer>
     * </runtime>
     * }
     * </pre>
     * Properties for Docker web app
     * <ul>
     *     <li> image: Name of the docker image to deploy. </li>
     *     <li> registryUrl: Docker repository of the image, could be omitted for docker hub. </li>
     *     <li> serverId: The authentication profile id in maven settings.xml. For private docker image,
     *     please set your username and password in maven settings.xml and refer it with `serverId` in runtime configuration. </li>
     * </ul>
     * <pre>
     * {@code
     * <runtime>
     *     <os>docker</os>
     *     <image>[hub-user/]repo-name[:tag]</image>
     *     <serverId></serverId>
     *     <registryUrl></registryUrl> <!- could be omitted for docker hub images -->
     * </runtime>
     * }
     * </pre>
     * @since 1.4.0
     */
    @JsonProperty
    @Parameter(property = "runtime")
    protected MavenRuntimeConfig runtime;

    /**
     * Configuration to specify the artifacts to deploy <p>
     * Parameters for resource
     * <ul>
     *     <li> type: Specifies where the resource type of the files to be deployed, valid values are: `jar`, `war`, `ear`,
     *     `lib`, `static`, `startup`, `zip` and `script`. </li>
     *     <li> directory: Specifies where the resources are stored. </li>
     *     <li> targetPath: Specifies the target path where the resources will be deployed to. </li>
     *     <li> includes: A list of patterns to include, e.g. `*.jar`. </li>
     *     <li> excludes: A list of patterns to exclude, e.g. `*.xml`. </li>
     * </ul>
     * <pre>
     * {@code
     * <deployment>
     *     <resources>
     *         <resource>
     *             <type>jar</type>
     *             <directory>./target</directory>
     *             <includes>
     *                 <include>*.jar</include>
     *             </includes>
     *         </resource>
     *   </resources>
     * </deployment>
     * }
     * </pre>
     * @since 2.0.0
     */
    @JsonProperty
    @Parameter(property = "deployment")
    protected Deployment deployment;

    @JsonIgnore
    protected File stagingDirectory;

    @JsonIgnore
    protected AzureAppService az;

    @JsonIgnore
    private boolean isRuntimeInjected = false;

    @JsonIgnore
    @Getter
    protected ConfigParser configParser = new ConfigParser(this);

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
        final boolean isDeployToSlot = Optional.ofNullable(getDeploymentSlotSetting()).map(DeploymentSlotSetting::getName)
                .map(StringUtils::isNotEmpty).orElse(false);
        map.put(DEPLOY_TO_SLOT_KEY, String.valueOf(isDeployToSlot));

        map.put(SKIP_CREATE_RESOURCE_KEY, String.valueOf(skipAzureResourceCreate || skipCreateAzureResource));
        return map;
    }

    protected void validateConfiguration(Consumer<ValidationMessage> validationMessageConsumer, boolean failOnError) {
        final List<ValidationMessage> validate = SchemaValidator.getInstance().validate("WebAppConfiguration", this, "configuration");
        validate.forEach(message -> validationMessageConsumer.accept(message));
        if (CollectionUtils.isNotEmpty(validate) && failOnError) {
            final String errorDetails = validate.stream().map(message -> message.getMessage().toString()).collect(Collectors.joining(StringUtils.LF));
            throw new AzureToolkitRuntimeException(String.join(StringUtils.LF, INVALID_PARAMETER_ERROR_MESSAGE, errorDetails));
        }
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
