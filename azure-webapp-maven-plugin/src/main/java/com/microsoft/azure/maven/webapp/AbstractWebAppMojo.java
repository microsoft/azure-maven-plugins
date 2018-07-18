/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Base abstract class for Web App Mojos.
 */
public abstract class AbstractWebAppMojo extends AbstractAppServiceMojo {
    public static final String JAVA_VERSION_KEY = "javaVersion";
    public static final String JAVA_WEB_CONTAINER_KEY = "javaWebContainer";
    public static final String LINUX_RUNTIME_KEY = "linuxRuntime";
    public static final String DOCKER_IMAGE_TYPE_KEY = "dockerImageType";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";

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
     *     <li>P1</li>
     *     <li>P2</li>
     *     <li>P3</li>
     * </ul>
     */
    @Parameter(property = "webapp.pricingTier", defaultValue = "S1")
    protected PricingTierEnum pricingTier;

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
     * Deployment type to deploy Web App. The plugin contains five types now:
     *
     * <ul>
     *      <li>FTP - {@code <resources>} specifies configurations for this kind of deployment.</li>
     *      <li>WAR - {@code <warFile>} and {@code <path>} specifies configurations for this kind of deployment.</li>
     *      <li>JAR - {@code <jarFile>} and {@code <path>} specifies configurations for this kind of deployment.</li>
     *      <li>AUTO - inspects {@code <packaging>} of the Maven project and uses WAR, JAR, or NONE </li>
     *      <li>NONE - does nothing</li>
     *      <li>* defaults to AUTO if nothing is specified</li>
     * <ul/>
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.deploymentType", defaultValue = "AUTO")
    protected String deploymentType;

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
    @Parameter
    protected List<Resource> resources = Collections.emptyList();

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
     * Deployment Slot. It will be created if it does not exist.
     * It requires the web app exists already.
     */
    @Parameter(alias = "deploymentSlot")
    protected DeploymentSlotSetting deploymentSlotSetting;

    //endregion

    //region Getter

    @Override
    protected boolean isSkipMojo() {
        return skip;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    public DeploymentSlotSetting getDeploymentSlotSetting() {
        return deploymentSlotSetting;
    }

    public String getAppServicePlanResourceGroup() {
        return appServicePlanResourceGroup;
    }

    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    public String getRegion() {
        return region;
    }

    public PricingTier getPricingTier() {
        return pricingTier == null ? PricingTier.STANDARD_S1 : pricingTier.toPricingTier();
    }

    public JavaVersion getJavaVersion() {
        return StringUtils.isEmpty(javaVersion) ? null : JavaVersion.fromString(javaVersion);
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

    public DeploymentType getDeploymentType() {
        return DeploymentType.fromString(deploymentType);
    }

    public boolean isStopAppDuringDeployment() {
        return stopAppDuringDeployment;
    }

    public String getDeploymentStageDirectory() {
        return Paths.get(getBuildDirectoryAbsolutePath(),
            "azure-webapps",
            getAppName()).toString();
    }

    public List<Resource> getResources() {
        return resources;
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

    //endregion

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
        map.put(JAVA_VERSION_KEY, StringUtils.isEmpty(javaVersion) ? "" : javaVersion);
        map.put(JAVA_WEB_CONTAINER_KEY, getJavaWebContainer().toString());
        map.put(LINUX_RUNTIME_KEY, StringUtils.isEmpty(linuxRuntime) ? "" : linuxRuntime);
        map.put(DOCKER_IMAGE_TYPE_KEY, WebAppUtils.getDockerImageType(getContainerSettings()).toString());
        map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType().toString());
        return map;
    }

    //endregion
}
