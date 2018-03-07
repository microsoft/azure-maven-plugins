/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base abstract class for Web App Mojos.
 */
public abstract class AbstractWebAppMojo extends AbstractAzureMojo {
    public static final String JAVA_VERSION_KEY = "javaVersion";
    public static final String JAVA_WEB_CONTAINER_KEY = "javaWebContainer";
    public static final String DOCKER_IMAGE_TYPE_KEY = "dockerImageType";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";

    //region Properties

    /**
     * Resource group of Web App. It will be created if it doesn't exist.
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.resourceGroup", required = true)
    protected String resourceGroup;

    /**
     * Web App name. It will be created if it doesn't exist.
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.appName", required = true)
    protected String appName;

    /**
     * Web App region, which will only be used to create Web App at the first time.
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.region", defaultValue = "westus")
    protected String region;

    /**
     * Web App pricing tier, which will only be used to create Web App at the first time.<br/>
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
     *
     * @since 0.1.0
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
     * Settings of docker container image within Web App. This only applies to Linux-based Web App.<br/>
     * Below are the supported sub-element within {@code <containerSettings>}:<br/>
     * {@code <useBuiltInImage>} specifies whether to use the built-in image in Web App on Linux<br/>
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
     * Application settings of Web App, in the form of name-value pairs.
     * <pre>
     * {@code
     * <appSettings>
     *         <property>
     *                 <name>setting-name</name>
     *                 <value>setting-value</value>
     *         </property>
     * </appSettings>
     * }
     * </pre>
     *
     * @since 0.1.0
     */
    @Parameter
    protected Properties appSettings;

    /**
     * Deployment type to deploy Web App. Only "ftp" is supported now.
     *
     * @since 0.1.0
     */
    @Parameter(property = "webapp.deploymentType", defaultValue = "ftp")
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
    protected List<Resource> resources;

    /**
     * Skip execution.
     *
     * @since 0.1.4
     */
    @Parameter(property = "webapp.skip", defaultValue = "false")
    protected boolean skip;

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

    public String getRegion() {
        return region;
    }

    public PricingTier getPricingTier() {
        return pricingTier == null ? PricingTier.STANDARD_S1 : pricingTier.toPricingTier();
    }

    public JavaVersion getJavaVersion() {
        return StringUtils.isEmpty(javaVersion) ? null : JavaVersion.fromString(javaVersion);
    }

    public WebContainer getJavaWebContainer() {
        return StringUtils.isEmpty(javaWebContainer)
                ? WebContainer.TOMCAT_8_5_NEWEST
                : WebContainer.fromString(javaWebContainer);
    }

    public ContainerSetting getContainerSettings() {
        return containerSettings;
    }

    public Map getAppSettings() {
        return appSettings;
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

    //endregion

    //region Telemetry Configuration Interface

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = super.getTelemetryProperties();
        map.put(JAVA_VERSION_KEY, StringUtils.isEmpty(javaVersion) ? "" : javaVersion);
        map.put(JAVA_WEB_CONTAINER_KEY, getJavaWebContainer().toString());
        map.put(DOCKER_IMAGE_TYPE_KEY, WebAppUtils.getImageType(getContainerSettings()).toString());
        map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType().toString());
        return map;
    }

    //endregion
}
