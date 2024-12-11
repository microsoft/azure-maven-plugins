/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.containerapps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.containerapps.config.AppContainerMavenConfig;
import com.microsoft.azure.maven.containerapps.config.IngressMavenConfig;
import com.microsoft.azure.maven.containerapps.parser.ConfigParser;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.containerapps.config.ContainerAppConfig;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.config.ContainerRegistryConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;
import java.util.Optional;

public abstract class AbstractMojoBase extends AbstractAzureMojo {
    private static final String PROXY = "proxy";
    public static final String TELEMETRY_KEY_SUBSCRIPTION_ID = "subscriptionId";
    public static final String TELEMETRY_KEY_PLUGIN_NAME = "pluginName";
    public static final String TELEMETRY_KEY_PLUGIN_VERSION = "pluginVersion";
    public static final String TELEMETRY_KEY_JAVA_VERSION = "javaVersion";

    /**
     * Name of the resource group
     */
    @Getter
    @Parameter(property = "resourceGroup")
    protected String resourceGroup;

    /**
     * Name of the app environment
     */
    @Getter
    @Parameter(property = "appEnvironmentName")
    protected String appEnvironmentName;

    /**
     * Name of the container app. It will be created if not exist
     */
    @Getter
    @Parameter(property = "appName")
    protected String appName;

    /**
     * Region of the container apps
     */
    @Getter
    @Parameter(property = "region")
    protected String region;

    //todo init registry and change the type
    @Getter
    @Parameter(property = "registry")
    protected ContainerRegistryConfig registry;

    @Getter
    @Parameter(property = "identity")
    protected String identity;

    @Getter
    @Parameter(property = "containers")
    protected List<AppContainerMavenConfig> containers;

    @Getter
    @Parameter(property = "ingress")
    protected IngressMavenConfig ingress;

    @Getter
    @Parameter(property = "scale")
    protected ContainerAppDraft.ScaleConfig scale;

    @JsonIgnore
    @Getter
    protected final ConfigParser configParser = new ConfigParser(this);


    protected void initTelemetryProxy() {
        super.initTelemetryProxy();
        final String javaVersion = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_PLUGIN_NAME, plugin.getArtifactId());
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_PLUGIN_VERSION, plugin.getVersion());
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_JAVA_VERSION, javaVersion);
        telemetryProxy.addDefaultProperty(PROXY, String.valueOf(ProxyManager.getInstance().isProxyEnabled()));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_SUBSCRIPTION_ID, Optional.ofNullable(getSubscriptionId()).orElse(StringUtils.EMPTY));
    }

    public synchronized ContainerAppConfig getConfiguration() {
        return this.getConfigParser().getContainerAppConfig();
    }
}
