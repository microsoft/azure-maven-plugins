/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.appservice.DockerImageType;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.parser.ConfigurationParser;
import com.microsoft.azure.maven.webapp.parser.V1ConfigurationParser;
import com.microsoft.azure.maven.webapp.parser.V2ConfigurationParser;
import com.microsoft.azure.maven.webapp.validator.V1ConfigurationValidator;
import com.microsoft.azure.maven.webapp.validator.V2ConfigurationValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

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
     * JVM version of Web App. This only applies to Windows-based Web App.<br/>
     * Below is the list of supported JVM versions:
     * <ul>
     *     <li>1.7</li>
     *     <li>1.7.0_51</li>
     *     <li>1.7.0_71</li>
     *     <li>1.8</li>
     *     <li>1.8.0_25</li>
     *     <li>1.8.0_60</li>
     *     <li>1.8.0_73</li>
     *     <li>1.8.0_111</li>
     *     <li>1.8.0_92</li>
     *     <li>1.8.0_102</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.javaVersion")
    protected String javaVersion;

    /**
     * Web container type and version within Web App. This only applies to Windows-based Web App.<br/>
     * Below is the list of supported web container types:
     * <ul>
     *     <li>tomcat 7.0</li>
     *     <li>tomcat 7.0.50</li>
     *     <li>tomcat 7.0.62</li>
     *     <li>tomcat 8.0</li>
     *     <li>tomcat 8.0.23</li>
     *     <li>tomcat 8.5</li>
     *     <li>tomcat 8.5.6</li>
     *     <li>jetty 9.1</li>
     *     <li>jetty 9.1.0.20131115</li>
     *     <li>jetty 9.3</li>
     *     <li>jetty 9.3.12.20161014</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.javaWebContainer", defaultValue = "tomcat 8.5")
    protected String javaWebContainer;

    /**
     * Below is the list of supported Linux runtime:
     * <ul>
     *     <li>tomcat 8.5-jre8</li>
     *     <li>tomcat 9.0-jre8</li>
     *     <li>jre8</li>
     * </ul>
     */
    @Parameter(property = "webapp.linuxRuntime")
    protected String linuxRuntime;

    /**
     * Settings of docker container image within Web App. This only applies to Linux-based Web App.<br/>
     * Below are the supported sub-element within {@code <containerSettings>}:<br/>
     * {@code <imageName>} specifies docker image name to use in Web App on Linux<br/>
     * {@code <serverId>} specifies credentials to access docker image. Use it when you are using private Docker Hub
     * image or private registry.<br/>
     * {@code <registryUrl>} specifies your docker image registry URL. Use it when you are using private registry.
     *
     * @since 0.1.0
     */
    @Parameter
    protected ContainerSetting containerSettings;

    /**
     * Flag to control whether stop Web App during deployment.
     *
     * @since 0.1.4
     */
    @Parameter(property = "webapp.stopAppDuringDeployment", defaultValue = "false")
    protected boolean stopAppDuringDeployment;

    /**
     * Resources to deploy to Web App.
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.resources")
    protected List<Resource> resources;

    /**
     * Skip execution.
     *
     * @since 0.1.4
     */
    @Parameter(property = "webapp.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Location of the war file which is going to be deployed. If this field is not defined,
     * plugin will find the war file with the final name in the build directory.
     *
     * @since 1.1.0
     */
    @Parameter(property = "webapp.warFile")
    protected String warFile;

    /**
     * Location of the jar file which is going to be deployed. If this field is not defined,
     * plugin will find the jar file with the final name in the build directory.
     *
     * @since 1.3.0
     */
    @Parameter(property = "webapp.jarFile")
    protected String jarFile;

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
     * Schema version, which will be used to indicate the version of settings schema to use.
     *
     * @since 2.0.0
     */
    @Parameter(property = "schemaVersion", defaultValue = "v1")
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

    public String getJavaVersion() {
        return this.javaVersion;
    }

    public String getLinuxRuntime() {
        return linuxRuntime;
    }

    public WebContainer getJavaWebContainer() {
        return StringUtils.isEmpty(javaWebContainer) ?
            WebContainer.TOMCAT_8_5_NEWEST :
            WebContainer.fromString(javaWebContainer);
    }

    public ContainerSetting getContainerSettings() {
        return containerSettings;
    }

    public boolean isStopAppDuringDeployment() {
        return stopAppDuringDeployment;
    }

    @Override
    public List<Resource> getResources() {
        return resources == null ? Collections.EMPTY_LIST : resources;
    }

    public String getWarFile() {
        return warFile;
    }

    public String getJarFile() {
        return jarFile;
    }

    public String getPath() {
        return path;
    }

    public WebApp getWebApp() throws AzureAuthFailureException {
        return getAzureClient().webApps().getByResourceGroup(getResourceGroup(), getAppName());
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
                return new V1ConfigurationParser(this, new V1ConfigurationValidator(this));
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
                StringUtils.isNotEmpty(webAppConfig.getServerId()), webAppConfig.getRegistryUrl()).toString();
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

        try {
            map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType().toString());
        } catch (AzureExecutionException e) {
            map.put(DEPLOYMENT_TYPE_KEY, "Unknown deployment type.");
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
