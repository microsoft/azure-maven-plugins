/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Parameter;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.parser.ConfigurationParser;
import com.microsoft.azure.maven.webapp.parser.V2ConfigurationParser;
import com.microsoft.azure.maven.webapp.validator.V2ConfigurationValidator;

/**
 * Base abstract class for Web App Mojos.
 */
public abstract class AbstractWebAppMojo extends AbstractAppServiceMojo {
    public static final String JAVA_VERSION_KEY = "javaVersion";
    public static final String JAVA_WEB_CONTAINER_KEY = "javaWebContainer";
    public static final String LINUX_RUNTIME_KEY = "linuxRuntime";
    public static final String DOCKER_IMAGE_TYPE_KEY = "dockerImageType";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
    public static final String OS_KEY = "os";
    public static final String INVALID_CONFIG_KEY = "invalidConfiguration";
    public static final String SCHEMA_VERSION_KEY = "schemaVersion";

    //region Properties

    /**
     * App Service pricing tier, which will only be used to create Web App at the first time.<br/>
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
    @Parameter(property = "webapp.pricingTier")
    protected String pricingTier;

    /**
     * Flag to control whether stop Web App during deployment.
     *
     * @since 0.1.4
     */
    @Parameter(property = "webapp.stopAppDuringDeployment", defaultValue = "false")
    protected boolean stopAppDuringDeployment;

    /**
     * Skip execution.
     *
     * @since 0.1.4
     */
    @Parameter(property = "webapp.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * The context path for the deployment.
     * By default it will be deployed to '/', which is also known as the ROOT.
     *
     * @since 1.1.0
     */
    @Parameter(property = "webapp.path", defaultValue = "/")
    protected String path;

    /**
     * App Service region, which will only be used to create App Service at the first time.
     */
    @Parameter(property = "webapp.region")
    protected String region;

    /**
     * Deployment Slot. It will be created if it does not exist.
     * It requires the web app exists already.
     */
    @Parameter(alias = "deploymentSlot")
    protected DeploymentSlotSetting deploymentSlotSetting;

    /**
     * Schema version, which will be used to indicate the version of settings schema to use.
     *
     * @since 2.0.0
     */
    @Parameter(property = "schemaVersion", defaultValue = "v2")
    protected String schemaVersion;

    /**
     * Runtime setting
     *
     * @since 2.0.0
     */
    @Parameter(property = "runtime")
    protected RuntimeSetting runtime;

    /**
     * Deployment setting
     *
     * @since 2.0.0
     */
    @Parameter(property = "deployment")
    protected Deployment deployment;

    private WebAppConfiguration webAppConfiguration;

    protected File stagingDirectory;

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

    public DeploymentSlotSetting getDeploymentSlotSetting() {
        return deploymentSlotSetting;
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


    public boolean isStopAppDuringDeployment() {
        return stopAppDuringDeployment;
    }

    @Override
    public List<Resource> getResources() {
        return this.deployment == null ? Collections.EMPTY_LIST : this.deployment.getResources();
    }

    public WebApp getWebApp() throws AzureAuthFailureException {
        try {
            return getAzureClient().webApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (AzureAuthFailureException authEx) {
            throw authEx;
        } catch (Exception ex) {
            // Swallow exception for non-existing web app
        }
        return null;
    }

    public DeploymentSlot getDeploymentSlot(final WebApp app, final String slotName) {
        DeploymentSlot slot = null;
        if (StringUtils.isNotEmpty(slotName)) {
            try {
                slot = app.deploymentSlots().getByName(slotName);
            } catch (NoSuchElementException deploymentSlotNotExistException) {
            }
        }
        return slot;
    }

    public boolean isDeployToDeploymentSlot() {
        return getDeploymentSlotSetting() != null;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public RuntimeSetting getRuntime() {
        return runtime;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public void setRuntime(final RuntimeSetting runtime) {
        this.runtime = runtime;
    }

    //endregion

    protected ConfigurationParser getParserBySchemaVersion() throws AzureExecutionException {
        final String version = StringUtils.isEmpty(getSchemaVersion()) ? "v1" : getSchemaVersion();

        switch (version.toLowerCase(Locale.ENGLISH)) {
            case "v1":
            	throw new AzureExecutionException(SchemaVersion.V1_SCHEMA_DEPRECATED);
            case "v2":
                return new V2ConfigurationParser(this, new V2ConfigurationValidator(this));
            default:
                throw new AzureExecutionException(SchemaVersion.UNKNOWN_SCHEMA_VERSION);
        }
    }

    protected WebAppConfiguration getWebAppConfiguration() throws AzureExecutionException {
        if (webAppConfiguration == null) {
            webAppConfiguration = getParserBySchemaVersion().getWebAppConfiguration();
        }
        return webAppConfiguration;
    }

    //region Setter

    // Set method to get value from configuration.
    // Required by maven plugin testing package when use @Parameter(alias="").
    // And the name has to be "set<Alias>"
    public void setDeploymentSlot(DeploymentSlotSetting slotSetting) {
        this.deploymentSlotSetting = slotSetting;
    }

    //endregion

    //region Telemetry Configuration Interface

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = super.getTelemetryProperties();
        final WebAppConfiguration webAppConfig;
        try {
            webAppConfig = getWebAppConfiguration();
        } catch (Exception e) {
            map.put(INVALID_CONFIG_KEY, e.getMessage());
            return map;
        }
        if (webAppConfig.getImage() != null) {
            final String imageType = AppServiceUtils.getDockerImageType(webAppConfig.getImage(),
                StringUtils.isNotBlank(webAppConfig.getServerId()), webAppConfig.getRegistryUrl()).toString();
            map.put(DOCKER_IMAGE_TYPE_KEY, imageType);
        } else {
            map.put(DOCKER_IMAGE_TYPE_KEY, DockerImageType.NONE.toString());
        }
        map.put(SCHEMA_VERSION_KEY, schemaVersion);
        map.put(OS_KEY, webAppConfig.getOs() == null ? "" : webAppConfig.getOs().toString());
        map.put(JAVA_VERSION_KEY, webAppConfig.getJavaVersion() == null ? "" :
            webAppConfig.getJavaVersion().toString());
        map.put(JAVA_WEB_CONTAINER_KEY, webAppConfig.getWebContainer() == null ? "" :
            webAppConfig.getJavaVersion().toString());
        map.put(LINUX_RUNTIME_KEY, webAppConfig.getRuntimeStack() == null ? "" :
            webAppConfig.getRuntimeStack().stack() + " " + webAppConfig.getRuntimeStack().version());
        if (StringUtils.isNoneBlank(getDeploymentType())) {
        	map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType());
        }

        return map;
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
