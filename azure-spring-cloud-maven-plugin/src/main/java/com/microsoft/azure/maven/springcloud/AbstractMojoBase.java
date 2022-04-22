/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.springcloud.config.AppDeploymentMavenConfig;
import com.microsoft.azure.maven.springcloud.config.ConfigurationParser;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;

public abstract class AbstractMojoBase extends AbstractAzureMojo {
    private static final String PROXY = "proxy";
    public static final String TELEMETRY_KEY_PUBLIC = "public";
    public static final String TELEMETRY_KEY_RUNTIME_VERSION = "runtimeVersion";
    public static final String TELEMETRY_KEY_CPU = "cpu";
    public static final String TELEMETRY_KEY_MEMORY = "memory";
    public static final String TELEMETRY_KEY_INSTANCE_COUNT = "instanceCount";
    public static final String TELEMETRY_KEY_JVM_OPTIONS = "jvmOptions";
    public static final String TELEMETRY_KEY_WITHIN_PARENT_POM = "isExecutedWithinParentPom";
    public static final String TELEMETRY_KEY_SUBSCRIPTION_ID = "subscriptionId";
    public static final String TELEMETRY_KEY_JAVA_VERSION = "javaVersion";

    /**
     * Whether user modify their pom file with azure-spring:config
     */
    public static final String TELEMETRY_KEY_POM_FILE_MODIFIED = "isPomFileModified";

    public static final String TELEMETRY_KEY_AUTH_METHOD = "authMethod";

    public static final String TELEMETRY_VALUE_AUTH_POM_CONFIGURATION = "Pom Configuration";
    public static final String TELEMETRY_KEY_PLUGIN_NAME = "pluginName";
    public static final String TELEMETRY_KEY_PLUGIN_VERSION = "pluginVersion";

    /**
     * Boolean flag to control whether the app exposes public endpoint
     */
    @Getter
    @Parameter(alias = "public")
    protected Boolean isPublic;

    /**
     * Name of the spring cloud service
     */
    @Getter
    @Parameter(property = "clusterName")
    protected String clusterName;

    /**
     * Name of the spring cloud app. It will be created if not exist
     */
    @Getter
    @Parameter(property = "appName")
    protected String appName;

    /**
     * Runtime version of the spring cloud app, supported values are `Java 8` and `Java 11`
     */
    @Getter
    @Parameter(property = "runtimeVersion")
    protected String runtimeVersion;

    /**
     * Configuration for spring cloud deployment
     *
     * Parameters for deployment
     * <ul>
     *     <li>cpu: Core numbers for deployment. </li>
     *     <li>memoryInGB: Memory for deployment. </li>
     *     <li>instanceCount: Instance count for deployment. </li>
     *     <li>deploymentName: Name for deployment. </li>
     *     <li>jvmOptions: JVM options for the deployed app. </li>
     *     <li>runtimeVersion: The runtime version for spring cloud app,  supported values are `Java 8` and `Java 11`. </li>
     *     <li>enablePersistentStorage: Boolean flag to control whether or not to mount a persistent storage to /persistent folder(volume quota of 50 GB). </li>
     *     <li>environment: Environment variables for deployment. </li>
     *     <li>resources: Configuration to specify the artifacts to deploy
     *     <ul>
     *         <li>directory: Specifies where the resources are stored.</li>
     *         <li>includes: A list of patterns to include, e.g. '*.jar'.</li>
     *         <li>excludes: A list of patterns to exclude, e.g. '*.xml'.</li>
     *     </ul>
     *     </li>
     * </ul>
     */
    @Getter
    @Parameter(property = "deployment")
    protected AppDeploymentMavenConfig deployment;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    protected void initTelemetryProxy() {
        super.initTelemetryProxy();
        final SpringCloudAppConfig configuration = this.getConfiguration();
        final String javaVersion = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_PLUGIN_NAME, plugin.getArtifactId());
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_PLUGIN_VERSION, plugin.getVersion());
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_WITHIN_PARENT_POM, String.valueOf(project.getPackaging().equalsIgnoreCase("pom")));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_JAVA_VERSION, javaVersion);

        telemetryProxy.addDefaultProperty(PROXY, String.valueOf(ProxyManager.getInstance().isProxyEnabled()));

        // Todo update deploy mojo telemetries with real value
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_AUTH_METHOD, TELEMETRY_VALUE_AUTH_POM_CONFIGURATION);

        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_PUBLIC, String.valueOf(configuration.isPublic()));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_RUNTIME_VERSION, configuration.getRuntimeVersion());
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_CPU, String.valueOf(configuration.getDeployment().getCpu()));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_MEMORY, String.valueOf(configuration.getDeployment().getMemoryInGB()));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_INSTANCE_COUNT, String.valueOf(configuration.getDeployment().getInstanceCount()));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_JVM_OPTIONS,
                String.valueOf(StringUtils.isEmpty(configuration.getDeployment().getJvmOptions())));
        telemetryProxy.addDefaultProperty(TELEMETRY_KEY_SUBSCRIPTION_ID, configuration.getSubscriptionId());
    }

    public SpringCloudAppConfig getConfiguration() {
        final ConfigurationParser parser = ConfigurationParser.getInstance();
        return parser.parse(this);
    }
}
