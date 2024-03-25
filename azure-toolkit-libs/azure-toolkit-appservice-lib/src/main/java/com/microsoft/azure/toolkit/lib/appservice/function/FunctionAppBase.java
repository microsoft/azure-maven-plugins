/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.serializer.SerializerFactory;
import com.azure.core.util.serializer.SerializerAdapter;
import com.azure.core.util.serializer.SerializerEncoding;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.WebSiteManagementClient;
import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.azure.resourcemanager.appservice.models.PlatformArchitecture;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.deploy.FTPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.FlexFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.IFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.MSFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.RunFromBlobFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.RunFromZipFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.ZIPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.file.AzureFunctionsAdminClient;
import com.microsoft.azure.toolkit.lib.appservice.file.IFileClient;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class FunctionAppBase<T extends FunctionAppBase<T, P, F>, P extends AbstractAzResource<P, ?, ?>, F extends WebAppBase>
    extends AppServiceAppBase<T, P, F> {
    private static final String FUNCTION_DEPLOY_TYPE = "functionDeployType";
    public static final String JAVA_OPTS = "JAVA_OPTS";
    public static final String HTTP_PLATFORM_DEBUG_PORT = "HTTP_PLATFORM_DEBUG_PORT";
    public static final String PREFER_IPV_4_STACK_TRUE = "-Djava.net.preferIPv4Stack=true";
    public static final String XDEBUG = "-Xdebug";
    public static final String XRUNJDWP = "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%s";
    public static final String DEFAULT_REMOTE_DEBUG_PORT = "8898";
    public static final String DEFAULT_REMOTE_DEBUG_JAVA_OPTS = "-Djava.net.preferIPv4Stack=true -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%s";

    private Boolean isEnableRemoteDebugging = null;
    private AzureFunctionsAdminClient fileClient;

    protected FunctionAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, F> module) {
        super(name, resourceGroupName, module);
    }

    protected FunctionAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, F> module) {
        super(name, module);
    }

    protected FunctionAppBase(@Nonnull T origin) {
        super(origin);
    }

    public void deploy(File targetFile) {
        deploy(targetFile, getDefaultDeployType());
    }

    public void deploy(File targetFile, FunctionDeployType functionDeployType) {
        OperationContext.action().setTelemetryProperty(FUNCTION_DEPLOY_TYPE, functionDeployType.name());
        getDeployHandlerByType(functionDeployType).deploy(targetFile, this);
    }

    public AzureFunctionsAdminClient getAdminClient() {
        if (fileClient == null) {
            fileClient = Optional.ofNullable(this.getRemote()).map(r -> AzureFunctionsAdminClient.getClient(r, this)).orElse(null);
        }
        return fileClient;
    }

    @Override
    public FunctionAppRuntime getRuntime() {
        return Optional.ofNullable(this.getRemote()).map(r -> {
            if (r.operatingSystem() == com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS) {
                final JavaVersion javaVersion = r.javaVersion();
                return FunctionAppWindowsRuntime.fromJavaVersion(javaVersion);
            } else {
                final String fxString = r.linuxFxVersion();
                if (StringUtils.isEmpty(fxString)) {
                    final boolean isFlexConsumption = Optional.ofNullable(getAppServicePlan())
                        .map(AppServicePlan::getPricingTier)
                        .map(PricingTier::isFlexConsumption).orElse(false);
                    return isFlexConsumption ? getFlexConsumptionRuntime() : null;
                } else if (StringUtils.startsWithIgnoreCase(fxString, "docker")) {
                    return FunctionAppDockerRuntime.INSTANCE;
                }
                return FunctionAppLinuxRuntime.fromFxString(fxString);
            }
        }).orElse(null);
    }

    private FunctionAppRuntime getFlexConsumptionRuntime() {
        final FunctionAppConfig config = getFlexConsumptionAppConfig();
        return Optional.ofNullable(config).map(FunctionAppConfig::getRuntime).map(FunctionAppConfig.FunctionsRuntime::getVersion)
            .map(FunctionAppLinuxRuntime::fromJavaVersionUserText)
            .orElse(null);
    }

    @Nullable
    protected IFileClient getFileClient() {
        // kudu api does not applies to linux consumption, using functions admin api instead
        final boolean isLinuxJavaFunction = Optional.ofNullable(getRuntime()).map(Runtime::isLinux).orElse(false);
        final boolean isConsumption = Optional.ofNullable(getAppServicePlan()).map(AppServicePlan::getPricingTier)
            .map(pricingTier -> pricingTier.isConsumption() || pricingTier.isFlexConsumption()).orElse(false);
        return (isLinuxJavaFunction && isConsumption) ? this.getAdminClient() : super.getFileClient();
    }

    protected FunctionDeployType getDefaultDeployType() {
        final PricingTier pricingTier = Optional.ofNullable(getAppServicePlan()).map(AppServicePlan::getPricingTier).orElse(PricingTier.PREMIUM_P1V2);
        final OperatingSystem os = Optional.ofNullable(getRuntime()).map(Runtime::getOperatingSystem).orElse(OperatingSystem.LINUX);
        if (pricingTier.isFlexConsumption()) {
            return FunctionDeployType.FLEX;
        }
        if (os == OperatingSystem.WINDOWS) {
            return FunctionDeployType.RUN_FROM_ZIP;
        }
        return StringUtils.equalsAnyIgnoreCase(pricingTier.getTier(), "Dynamic", "ElasticPremium") ?
            FunctionDeployType.RUN_FROM_BLOB : FunctionDeployType.RUN_FROM_ZIP;
    }

    protected IFunctionDeployHandler getDeployHandlerByType(final FunctionDeployType deployType) {
        switch (deployType) {
            case FLEX:
                return new FlexFunctionDeployHandler();
            case FTP:
                return new FTPFunctionDeployHandler();
            case ZIP:
                return new ZIPFunctionDeployHandler();
            case MSDEPLOY:
                return new MSFunctionDeployHandler();
            case RUN_FROM_ZIP:
                return new RunFromZipFunctionDeployHandler();
            case RUN_FROM_BLOB:
                return new RunFromBlobFunctionDeployHandler();
            default:
                throw new AzureToolkitRuntimeException("Unsupported deployment type");
        }
    }

    public abstract String getMasterKey();

    public boolean isRemoteDebugEnabled() {
        if (isEnableRemoteDebugging == null) {
            isEnableRemoteDebugging = getIsRemoteDebuggingEnabled();
        }
        return isEnableRemoteDebugging;
    }

    public abstract void enableRemoteDebug();

    public abstract void disableRemoteDebug();

    public void ping() {
        getAdminClient().ping();
    }

    protected String getRemoteDebugPort() {
        return DEFAULT_REMOTE_DEBUG_PORT;
    }

    public String getJavaOptsWithRemoteDebugDisabled(final Map<String, String> appSettings) {
        final String javaOpts = appSettings.get(JAVA_OPTS);
        if (StringUtils.isEmpty(javaOpts)) {
            return String.format(StringUtils.EMPTY);
        }
        return Arrays.stream(javaOpts.split(" "))
                .filter(opts -> !StringUtils.containsAnyIgnoreCase(opts, PREFER_IPV_4_STACK_TRUE, XDEBUG, "-Xrunjdwp"))
                .collect(Collectors.joining(" "));
    }

    public String getJavaOptsWithRemoteDebugEnabled(final Map<String, String> appSettings, String debugPort) {
        final String javaOpts = appSettings.get(JAVA_OPTS);
        if (StringUtils.isEmpty(javaOpts)) {
            return String.format(DEFAULT_REMOTE_DEBUG_JAVA_OPTS, debugPort);
        }
        final List<String> jvmOptions = new ArrayList<>(Arrays.asList(javaOpts.split(" ")));
        final String jdwp = String.format(XRUNJDWP, debugPort);
        for (final String configuration : Arrays.asList(PREFER_IPV_4_STACK_TRUE, XDEBUG, jdwp)) {
            if (!jvmOptions.contains(configuration)) {
                jvmOptions.add(configuration);
            }
        }
        return String.join(" ", jvmOptions);
    }

    public FunctionAppConfig getFlexConsumptionAppConfig(){
        // todo: return null if not flex consumption
        return Optional.ofNullable(getRemote()).map(remote -> {
            final HttpPipeline httpPipeline = remote.manager().httpPipeline();
            final String targetUrl = getRawRequestEndpoint(remote);
            final HttpRequest request = new HttpRequest(HttpMethod.GET, targetUrl)
                .setHeader(HttpHeaderName.CONTENT_TYPE, "application/json");
            try (final HttpResponse block = httpPipeline.send(request).block()) {
                final String content = Optional.ofNullable(block).map(HttpResponse::getBodyAsString).map(Mono::block).orElse(StringUtils.EMPTY);
                final SerializerAdapter adapter = SerializerFactory.createDefaultManagementSerializerAdapter();
                final ObjectNode functionNode = adapter.deserialize(content, ObjectNode.class, SerializerEncoding.JSON);
                final JsonNode configNode = Optional.ofNullable(functionNode.get("properties")).map(propertiesNode -> propertiesNode.get("functionAppConfig")).orElse(null);
                return Objects.isNull(configNode) ? (FunctionAppConfig) null : adapter.deserialize(configNode.toPrettyString(), FunctionAppConfig.class, SerializerEncoding.JSON);
            } catch (Throwable t) {
                return null;
            }
        }).orElse(null);
    }

    public String getRawRequestEndpoint(@Nonnull com.azure.resourcemanager.appservice.models.WebAppBase functionApp) {
        final AppServiceManager manager = functionApp.manager();
        final WebSiteManagementClient client = manager.serviceClient();
        final String endpoint = client.getEndpoint();
        // final String apiVersion = client.getApiVersion();
        // todo: change to use api version from client
        final String apiVersion = "2023-12-01";
        final String subscriptionId = client.getSubscriptionId();
        return endpoint + String.format("subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s?api-version=%s"
            , subscriptionId, functionApp.resourceGroupName(), functionApp.name(), apiVersion);
    }

    @Nullable
    public FlexConsumptionConfiguration getFlexConsumptionConfiguration() {
        return FlexConsumptionConfiguration.fromFunctionAppBase(this);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable F newRemote, @Nullable F oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        if (Objects.nonNull(newRemote)) {
            this.isEnableRemoteDebugging = getIsRemoteDebuggingEnabled();
        } else {
            this.isEnableRemoteDebugging = null;
        }
    }

    private boolean getIsRemoteDebuggingEnabled() {
        final WebAppBase remote = getRemote();
        if (Objects.isNull(remote)) {
            return false;
        }
        final Map<String, String> appSettings = Optional.ofNullable(this.getAppSettings()).orElse(Collections.emptyMap());
        // siteConfig for remote debug
        final boolean configEnabled = remote.webSocketsEnabled() && remote.platformArchitecture() == PlatformArchitecture.X64;
        // JAVA_OPTS
        final boolean appSettingsEnabled = appSettings.containsKey(HTTP_PLATFORM_DEBUG_PORT) &&
            StringUtils.equalsIgnoreCase(appSettings.get(JAVA_OPTS), getJavaOptsWithRemoteDebugEnabled(appSettings, appSettings.get(HTTP_PLATFORM_DEBUG_PORT)));
        return configEnabled && appSettingsEnabled;
    }

    @Override
    public boolean isStreamingLogSupported() {
        final OperatingSystem operatingSystem = Optional.ofNullable(getRuntime()).map(Runtime::getOperatingSystem).orElse(null);
        final boolean isEnableApplicationLog = Optional.ofNullable(getDiagnosticConfig())
                .map(DiagnosticConfig::isEnableApplicationLog).orElse(false);
        return operatingSystem != OperatingSystem.LINUX && isEnableApplicationLog;
    }
}
