/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.models.PlatformArchitecture;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.deploy.FTPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.IFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.MSFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.RunFromBlobFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.RunFromZipFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.deploy.ZIPFunctionDeployHandler;
import com.microsoft.azure.toolkit.lib.appservice.file.AzureFunctionsAdminClient;
import com.microsoft.azure.toolkit.lib.appservice.file.IFileClient;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    protected FunctionAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, WebSiteBase> module) {
        super(name, resourceGroupName, module);
    }

    protected FunctionAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, WebSiteBase> module) {
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
        getDeployHandlerByType(functionDeployType).deploy(targetFile, getFullRemote());
    }

    protected AzureFunctionsAdminClient getAdminClient() {
        if (fileClient == null) {
            fileClient = Optional.ofNullable(this.getFullRemote()).map(r -> AzureFunctionsAdminClient.getClient(r, this)).orElse(null);
        }
        return fileClient;
    }

    @Nullable
    protected IFileClient getFileClient() {
        // kudu api does not applies to linux consumption, using functions admin api instead
        return getAdminClient();
    }

    protected FunctionDeployType getDefaultDeployType() {
        final PricingTier pricingTier = Optional.ofNullable(getAppServicePlan()).map(AppServicePlan::getPricingTier).orElse(PricingTier.PREMIUM_P1V2);
        final OperatingSystem os = Optional.ofNullable(getRuntime()).map(Runtime::getOperatingSystem).orElse(OperatingSystem.LINUX);
        if (os == OperatingSystem.WINDOWS) {
            return FunctionDeployType.RUN_FROM_ZIP;
        }
        return StringUtils.equalsAnyIgnoreCase(pricingTier.getTier(), "Dynamic", "ElasticPremium") ?
            FunctionDeployType.RUN_FROM_BLOB : FunctionDeployType.RUN_FROM_ZIP;
    }

    protected IFunctionDeployHandler getDeployHandlerByType(final FunctionDeployType deployType) {
        switch (deployType) {
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

    @Override
    protected void updateAdditionalProperties(@Nullable WebSiteBase newRemote, @Nullable WebSiteBase oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        if (Objects.nonNull(newRemote)) {
            this.isEnableRemoteDebugging = getIsRemoteDebuggingEnabled();
        } else {
            this.isEnableRemoteDebugging = null;
        }
    }

    private boolean getIsRemoteDebuggingEnabled() {
        final F remote = Objects.requireNonNull(getFullRemote());
        final Map<String, String> appSettings = Objects.requireNonNull(this.getAppSettings());
        // siteConfig for remote debug
        final boolean configEnabled = remote.webSocketsEnabled() && remote.platformArchitecture() == PlatformArchitecture.X64;
        // JAVA_OPTS
        final boolean appSettingsEnabled = appSettings.containsKey(HTTP_PLATFORM_DEBUG_PORT) &&
                StringUtils.equalsIgnoreCase(appSettings.get(JAVA_OPTS), getJavaOptsWithRemoteDebugEnabled(appSettings, appSettings.get(HTTP_PLATFORM_DEBUG_PORT)));
        return configEnabled && appSettingsEnabled;
    }
}
