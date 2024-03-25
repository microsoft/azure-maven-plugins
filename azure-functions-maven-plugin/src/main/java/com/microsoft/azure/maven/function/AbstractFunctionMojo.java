/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.azure.core.implementation.SemanticVersion;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.microsoft.azure.maven.appservice.AbstractAppServiceMojo;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {

    protected static final String HOST_JSON = "host.json";
    protected static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    protected static final String TRIGGER_TYPE = "triggerType";
    protected static final String AZURE_FUNCTIONS_JAVA_LIBRARY = "azure-functions-java-library";
    protected static final String CAN_NOT_FIND_ARTIFACT = "Cannot find the maven artifact, please run `mvn package` first.";

    protected static final Map<FunctionExtensionVersion, Set<Integer>> FUNCTION_EXTENSION_LIBRARY_MAP = new HashMap<FunctionExtensionVersion, Set<Integer>>() {
        {
            put(FunctionExtensionVersion.VERSION_2, Sets.newHashSet(1, 2));
            put(FunctionExtensionVersion.VERSION_3, Sets.newHashSet(1, 2));
            put(FunctionExtensionVersion.VERSION_4, Sets.newHashSet(3));
        }
    };
    private static final String FUNCTION_JAVA_VERSION_KEY = "functionJavaVersion";
    private static final String DISABLE_APP_INSIGHTS_KEY = "disableAppInsights";
    private static final String FUNCTION_RUNTIME_KEY = "os";
    private static final String FUNCTION_IS_DOCKER_KEY = "isDockerFunction";
    private static final String FUNCTION_REGION_KEY = "region";
    private static final String FUNCTION_PRICING_KEY = "pricingTier";
    private static final String FUNCTION_DEPLOY_TO_SLOT_KEY = "isDeployToFunctionSlot";

    //region Properties
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    /**
     * Pricing for function app <p>
     * Supported values : CONSUMPTION, B1, B2, B3, S1, S2, S3, P1V2, P2V2, P3V2, P1V3, P2V3, P3V3, EP1, EP2, EP3
     */
    @Getter
    @Parameter(property = "functions.pricingTier")
    protected String pricingTier;

    /**
     * Boolean flag to skip the execution of maven plugin for azure functions
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.skip", defaultValue = "false")
    protected Boolean skip;

    /**
     * Region for function app
     * Supported values: westus, westus2, eastus, eastus2, northcentralus, southcentralus, westcentralus, canadacentral, canadaeast, brazilsouth, northeurope,
     * westeurope, uksouth, eastasia, southeastasia, japaneast, japanwest, australiaeast, australiasoutheast, centralindia, southindia ...
     *
     * @since 1.2.0
     */
    @Parameter(property = "functions.region")
    protected String region;

    /**
     * Runtime environment of function app <p>
     * Properties for Windows/Linux function app
     * <ul>
     *     <li> os: Operating system for the function App, default to be Windows. </li>
     *     <li> javaVersion: Java runtime version the function App, supported values are `Java 8`, `Java 11` and `Java 17`, default to be `Java 17`. </li>
     * </ul>
     * <pre>
     * {@code
     * <runtime>
     *     <os>windows</os>
     *     <javaVersion>Java 17</javaVersion>
     * </runtime>
     * }
     * </pre>
     * Properties for Docker function app
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
     *
     * @since 1.4.0
     */
    @Parameter(property = "functions.runtime")
    protected RuntimeConfiguration runtime;

    /**
     * Name of the application insight instance, must be in the same resource group with function app.
     * Will be skipped if `appInsightsKey` is specified
     *
     * @since 1.6.0
     */
    @Parameter(property = "functions.appInsightsInstance")
    protected String appInsightsInstance;

    /**
     * Instrumentation key of the application insights instance
     *
     * @since 1.6.0
     */
    @Parameter(property = "functions.appInsightsKey")
    protected String appInsightsKey;

    /**
     * Boolean flag to monitor the Function App with application insights
     *
     * @since 1.6.0
     */
    @Parameter(property = "functions.disableAppInsights", defaultValue = "false")
    protected Boolean disableAppInsights;

    /**
     * Boolean flag to control whether to enable distributed tracing for function app
     */
    @Getter
    @Parameter(property = "functions.enableDistributedTracing")
    protected Boolean enableDistributedTracing;

    /**
     * Path for host.json file
     *
     * @since 1.22.0
     */
    @Getter
    @Parameter(property = "functions.hostJson", defaultValue = HOST_JSON)
    protected String hostJson;

    /**
     * Path for local.settings.json file
     *
     * @since 1.22.0
     */
    @Getter
    @Parameter(property = "functions.localSettingsJson", defaultValue = LOCAL_SETTINGS_JSON)
    protected String localSettingsJson;

    /**
     * Path for the artifact to package and deploy
     */
    @Getter
    @Parameter(property = "functions.artifact")
    protected String artifactPath;

    /**
     * Name of the storage account. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Getter
    @Parameter
    protected String storageAccountName;

    /**
     * Resource group of storage account. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Getter
    @Parameter
    protected String storageAccountResourceGroup;

    /**
     * Name of the container app environment. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Getter
    @Parameter
    protected String environment;

    /**
     * The minimum number of replicas when create function app on container app.
     */
    @JsonProperty
    @Getter
    @Parameter
    protected Integer minReplicas;

    /**
     * The maximum number of replicas when create function app on container app.
     */
    @JsonProperty
    @Getter
    @Parameter
    protected Integer maxReplicas;

    //endregion

    //region Getter

    public String getRegion() {
        return region;
    }

    @Override
    protected boolean isSkipMojo() {
        return BooleanUtils.isTrue(skip);
    }

    public String getFinalName() {
        return finalName;
    }

    public String getAppInsightsInstance() {
        return appInsightsInstance;
    }

    public String getAppInsightsKey() {
        return appInsightsKey;
    }

    public boolean isDisableAppInsights() {
        return BooleanUtils.isTrue(disableAppInsights);
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtime;
    }

    @Nonnull
    protected File getArtifact() throws AzureToolkitRuntimeException {
        // Using artifact from configuration first
        if (StringUtils.isNotEmpty(getArtifactPath())) {
            return new File(getArtifactPath());
        }
        final Artifact artifact = project.getArtifact();
        if (artifact.getFile() != null) {
            return artifact.getFile();
        }
        // Get artifact by buildDirectory and finalName
        // as project.getArtifact() will be null when invoke azure-functions:package directly
        final String finalName = project.getBuild().getFinalName();
        final String packaging = project.getPackaging();
        final File result = new File(buildDirectory, StringUtils.join(finalName, FilenameUtils.EXTENSION_SEPARATOR, packaging));
        if (!result.exists()) {
            throw new AzureToolkitRuntimeException(CAN_NOT_FIND_ARTIFACT);
        }
        return result;
    }

    protected File getHostJsonFile() {
        final Path path = Paths.get(getHostJson());
        return path.isAbsolute() ? path.toFile() :
                Paths.get(project.getBasedir().getAbsolutePath(), getHostJson()).toFile();
    }

    protected File getLocalSettingsJsonFile() {
        final Path path = Paths.get(getLocalSettingsJson());
        return path.isAbsolute() ? path.toFile() :
                Paths.get(project.getBasedir().getAbsolutePath(), getLocalSettingsJson()).toFile();
    }

    protected void validateAppName() {
        if (StringUtils.isBlank(appName)) {
            throw new AzureToolkitRuntimeException("<appName> is not configured in pom");
        }
    }

    protected void validateFunctionCompatibility() {
        // get bundle version
        final FunctionExtensionVersion bundleVersion = getBundleVersion();
        // get function library version
        final String functionLibraryVersion = getFunctionLibraryVersion();
        final Integer functionLibraryMajorVersion = Optional.ofNullable(functionLibraryVersion)
                .map(version -> SemanticVersion.parse(version).getMajorVersion())
                .orElse(null);
        // todo: validate host json version
        if (bundleVersion == null || bundleVersion == FunctionExtensionVersion.UNKNOWN || functionLibraryMajorVersion == null) {
            return;
        }
        final Set<Integer> matchedVersions = FUNCTION_EXTENSION_LIBRARY_MAP.get(bundleVersion);
        if (!matchedVersions.contains(functionLibraryMajorVersion)) {
            final String validVersions = matchedVersions.stream().map(value -> String.format("v%s.*", value)).collect(Collectors.joining(","));
            AzureMessager.getMessager().warning("There might be some compatibility issues between azure function extension bundle and azure functions java library");
            AzureMessager.getMessager().warning(AzureString.format("Valid function library versions for extension bundle v%s should be: %s, current value is %s",
                    bundleVersion.getValue(), validVersions, functionLibraryVersion));
        }
    }

    @Nullable
    protected String getFunctionLibraryVersion() {
        final Set<Artifact> artifacts = project.getArtifacts();
        return artifacts.stream()
                .filter(artifact -> StringUtils.equals(artifact.getArtifactId(), AZURE_FUNCTIONS_JAVA_LIBRARY))
                .findFirst()
                .map(Artifact::getVersion)
                .orElse(null);
    }

    @Nullable
    protected JsonNode readHostJson() {
        // todo: add configuration for host.json location
        final File hostJson = getHostJsonFile();
        try (final FileInputStream fis = new FileInputStream(hostJson)) {
            final String content = IOUtils.toString(fis, Charset.defaultCharset());
            return JsonUtils.fromJson(content, JsonNode.class);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    protected FunctionExtensionVersion getBundleVersion() {
        return Optional.ofNullable(readHostJson())
                .map(jsonNode -> jsonNode.at("/extensionBundle/version"))
                .filter(jsonNode -> !jsonNode.isMissingNode())
                .map(jsonNode -> FunctionUtils.parseFunctionExtensionVersionFromHostJson(jsonNode.asText()))
                .orElse(null);
    }

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> result = super.getTelemetryProperties();
        final String javaVersion = runtime == null ? null : runtime.getJavaVersion();
        final String os = runtime == null ? null : runtime.getOs();
        final boolean isDockerFunction = runtime != null && StringUtils.isNotEmpty(runtime.getImage());
        result.put(FUNCTION_JAVA_VERSION_KEY, StringUtils.isEmpty(javaVersion) ? "" : javaVersion);
        result.put(FUNCTION_RUNTIME_KEY, StringUtils.isEmpty(os) ? "" : os);
        result.put(FUNCTION_IS_DOCKER_KEY, String.valueOf(isDockerFunction));
        result.put(FUNCTION_REGION_KEY, region);
        result.put(FUNCTION_PRICING_KEY, pricingTier);
        result.put(DISABLE_APP_INSIGHTS_KEY, String.valueOf(isDisableAppInsights()));
        final boolean isDeployToFunctionSlot = getDeploymentSlotSetting() != null && StringUtils.isNotEmpty(getDeploymentSlotSetting().getName());
        result.put(FUNCTION_DEPLOY_TO_SLOT_KEY, String.valueOf(isDeployToFunctionSlot));
        return result;
    }
    //endregion
}
