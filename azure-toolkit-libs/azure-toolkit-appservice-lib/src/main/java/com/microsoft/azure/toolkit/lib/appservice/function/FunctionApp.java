/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.FunctionAppBasic;
import com.azure.resourcemanager.appservice.models.PlatformArchitecture;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.ANONYMOUS;

@Getter
public class FunctionApp extends FunctionAppBase<FunctionApp, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.FunctionApp>
    implements Deletable {

    @Nonnull
    private final FunctionAppDeploymentSlotModule deploymentModule;
    private static final String SYNC_TRIGGERS = "Syncing triggers and fetching function information";
    private static final String UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS = "Some http trigger urls cannot be displayed " +
            "because they are non-anonymous. To access the non-anonymous triggers, please refer to https://aka.ms/azure-functions-key.";
    private static final String HTTP_TRIGGER_URLS = "HTTP Trigger Urls:";
    private static final String NO_ANONYMOUS_HTTP_TRIGGER = "No anonymous HTTP Triggers found in deployed function app, skip list triggers.";
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";
    private static final int SYNC_FUNCTION_MAX_ATTEMPTS = 5;
    private static final int SYNC_FUNCTION_DELAY = 1;
    private static final String LIST_TRIGGERS = "Querying triggers...";
    private static final String LIST_TRIGGERS_WITH_RETRY = "Querying triggers (Attempt {0}/{1})...";
    private static final String NO_TRIGGERS_FOUNDED = "No triggers found in deployed function app, " +
            "please try recompile the project by `mvn clean package` and deploy again.";
    private static final int LIST_TRIGGERS_MAX_RETRY = 5;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;

    protected FunctionApp(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull FunctionAppModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new FunctionAppDeploymentSlotModule(this);
    }

    /**
     * copy constructor
     */
    protected FunctionApp(@Nonnull FunctionApp origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
    }

    protected FunctionApp(@Nonnull FunctionAppBasic remote, @Nonnull FunctionAppModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.deploymentModule = new FunctionAppDeploymentSlotModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(deploymentModule);
    }

    @Nullable
    @Override
    public String getMasterKey() {
        return Optional.ofNullable(this.getFullRemote()).map(com.azure.resourcemanager.appservice.models.FunctionApp::getMasterKey).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/function.enable_remote_debugging.app", params = {"this.getName()"})
    public void enableRemoteDebug() {
        final Map<String, String> appSettings = Optional.ofNullable(this.getAppSettings()).orElseGet(HashMap::new);
        final String debugPort = appSettings.getOrDefault(HTTP_PLATFORM_DEBUG_PORT, getRemoteDebugPort());
        doModify(() -> Objects.requireNonNull(getFullRemote()).update()
                .withWebSocketsEnabled(true)
                .withPlatformArchitecture(PlatformArchitecture.X64)
                .withAppSetting(HTTP_PLATFORM_DEBUG_PORT, appSettings.getOrDefault(HTTP_PLATFORM_DEBUG_PORT, getRemoteDebugPort()))
                .withAppSetting(JAVA_OPTS, getJavaOptsWithRemoteDebugEnabled(appSettings, debugPort)).apply(), Status.UPDATING);
    }

    @Override
    @AzureOperation(name = "azure/function.disable_remote_debugging.app", params = {"this.getName()"})
    public void disableRemoteDebug() {
        final Map<String, String> appSettings = Objects.requireNonNull(this.getAppSettings());
        final String javaOpts = this.getJavaOptsWithRemoteDebugDisabled(appSettings);
        doModify(() -> {
            if (StringUtils.isEmpty(javaOpts)) {
                Objects.requireNonNull(getFullRemote()).update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withoutAppSetting(JAVA_OPTS).apply();
            } else {
                Objects.requireNonNull(getFullRemote()).update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withAppSetting(JAVA_OPTS, javaOpts).apply();
            }
        }, Status.UPDATING);
    }

    @Nonnull
    public List<FunctionEntity> listFunctions(boolean... force) {
        return Optional.ofNullable(this.getFullRemote()).map(r -> r.listFunctions().stream()
                .map(envelope -> AppServiceUtils.fromFunctionAppEnvelope(envelope, this.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    @AzureOperation(name = "azure/function.trigger_function.func", params = {"functionName"})
    public void triggerFunction(String functionName, Object input) {
        Optional.ofNullable(this.getFullRemote()).ifPresent(r -> r.triggerFunction(functionName, input));
    }

    @AzureOperation(name = "azure/function.swap_slot.app|slot", params = {"this.getName()", "slotName"})
    public void swap(String slotName) {
        this.doModify(() -> {
            Objects.requireNonNull(this.getFullRemote()).swap(slotName);
            AzureMessager.getMessager().info(AzureString.format("Swap deployment slot %s into production successfully", slotName));
        }, Status.UPDATING);
    }

    public void syncTriggers() {
        Optional.ofNullable(this.getFullRemote()).ifPresent(com.azure.resourcemanager.appservice.models.FunctionApp::syncTriggers);
    }

    @Nonnull
    public Map<String, String> listFunctionKeys(String functionName) {
        return Optional.ofNullable(this.getFullRemote()).map(r -> r.listFunctionKeys(functionName)).orElseGet(HashMap::new);
    }

    @Nonnull
    public FunctionAppDeploymentSlotModule slots() {
        return this.deploymentModule;
    }

    public void listHTTPTriggerUrls() throws Exception {
        final IAzureMessager messager = AzureMessager.getMessager();
        trySyncTriggers();
        final List<FunctionEntity> triggers = trySyncListFunctions();
        final List<FunctionEntity> httpFunction = triggers.stream()
                .filter(function -> function.getTrigger() != null &&
                        StringUtils.equalsIgnoreCase(function.getTrigger().getType(), HTTP_TRIGGER))
                .collect(Collectors.toList());
        final List<FunctionEntity> anonymousTriggers = httpFunction.stream()
                .filter(bindingResource -> bindingResource.getTrigger() != null &&
                        StringUtils.equalsIgnoreCase(bindingResource.getTrigger().getProperty(AUTH_LEVEL), ANONYMOUS))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
            messager.info(NO_ANONYMOUS_HTTP_TRIGGER);
            return;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(HTTP_TRIGGER_URLS).append(System.lineSeparator());
        anonymousTriggers.forEach(trigger -> builder.append(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())).append(System.lineSeparator()));
        if (anonymousTriggers.size() < httpFunction.size()) {
            builder.append(UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS);
        }
        messager.info(builder.toString());
    }

    // Refers https://github.com/Azure/azure-functions-core-tools/blob/3.0.3568/src/Azure.Functions.Cli/Actions/AzureActions/PublishFunctionAppAction.cs#L452
    private void trySyncTriggers() throws InterruptedException {
        AzureMessager.getMessager().info(SYNC_TRIGGERS);
        Thread.sleep(5 * 1000);
        Mono.fromRunnable(() -> {
                    try {
                        this.syncTriggers();
                    } catch (ManagementException e) {
                        if (e.getResponse().getStatusCode() != 200) { // Java SDK throw exception with 200 response, swallow exception in this case
                            throw e;
                        }
                    }
                }).subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.fixedDelay(SYNC_FUNCTION_MAX_ATTEMPTS - 1, Duration.ofSeconds(SYNC_FUNCTION_DELAY))).block();
    }

    private List<FunctionEntity> trySyncListFunctions() {
        final int[] count = {0};
        final IAzureMessager messager = AzureMessager.getMessager();
        return Mono.fromCallable(() -> {
                    final AzureString message = count[0]++ == 0 ? AzureString.fromString(LIST_TRIGGERS) : AzureString.format(LIST_TRIGGERS_WITH_RETRY, count[0], LIST_TRIGGERS_MAX_RETRY);
                    messager.info(message);
                    return Optional.of(this.listFunctions())
                            .filter(CollectionUtils::isNotEmpty)
                            .orElseThrow(() -> new AzureToolkitRuntimeException(NO_TRIGGERS_FOUNDED));
                }).subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.fixedDelay(LIST_TRIGGERS_MAX_RETRY - 1, Duration.ofSeconds(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS))).block();
    }

    @Nullable
    @Override
    protected WebSiteBase doModify(@Nonnull Callable<WebSiteBase> body, @Nullable String status) {
        // override only to provide package visibility
        return super.doModify(body, status);
    }
}
