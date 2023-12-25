/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.FunctionAppStackInner;
import com.azure.resourcemanager.appservice.models.FunctionAppMajorVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionsServiceSubscription extends AppServiceServiceSubscription {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
    };

    protected FunctionsServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureAppService service) {
        super(subscriptionId, service);
    }

    protected FunctionsServiceSubscription(@Nonnull AppServiceManager remote, @Nonnull AzureAppService service) {
        super(remote, service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.getFunctionAppModule());
    }

    @Nonnull
    public List<FunctionAppRuntime> listFunctionAppMajorRuntimes() {
        if (!FunctionAppWindowsRuntime.isLoaded() && !FunctionAppLinuxRuntime.isLoaded()) {
            loadRuntimes();
        }
        return Stream.concat(FunctionAppWindowsRuntime.getMajorRuntimes().stream(), FunctionAppLinuxRuntime.getMajorRuntimes().stream()).collect(Collectors.toList());
    }

    public synchronized void loadRuntimes() {
        loadRuntimesUsingHttpPipeline();
    }

    public synchronized void loadRuntimesUsingSdk() {
        final AppServiceManager remote = this.getRemote();
        if (Objects.isNull(remote)) {
            return;
        }
        if (FunctionAppWindowsRuntime.isLoaded() && FunctionAppLinuxRuntime.isLoaded()) {
            return;
        }

        final List<FunctionAppMajorVersion> javaStacks = remote.serviceClient().getProviders()
            .getFunctionAppStacksAsync().toStream()
            .filter(stack -> StringUtils.equalsIgnoreCase(stack.name(), "java"))
            .findFirst().map(FunctionAppStackInner::majorVersions).orElse(Collections.emptyList());

        // fill `Runtime` only with major versions
        FunctionAppLinuxRuntime.loadAllFunctionAppLinuxRuntimes(javaStacks);
        FunctionAppWindowsRuntime.loadAllFunctionAppWindowsRuntimes(javaStacks);
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadRuntimesUsingHttpPipeline() {
        final AppServiceManager remote = this.getRemote();
        if (Objects.isNull(remote)) {
            return;
        }
        if (FunctionAppWindowsRuntime.isLoaded() && FunctionAppLinuxRuntime.isLoaded()) {
            return;
        }
        final Map<String, Object> result = getRuntimesUsingHttpPipeline(remote);
        if (result.isEmpty()) {
            return;
        }
        final List<Map<String, Object>> stacksList = Utils.get(result, "$.value");
        final List<Map<String, Object>> javaStacks = (List<Map<String, Object>>) Objects.requireNonNull(stacksList).stream()
            .filter(s -> StringUtils.equalsIgnoreCase(Utils.get(s, "$.name"), "java"))
            .findFirst().map(j -> Utils.get(j, "$.properties.majorVersions")).orElse(Collections.emptyList());

        // fill `Runtime` only with major versions
        FunctionAppLinuxRuntime.loadAllFunctionAppLinuxRuntimesFromMap(javaStacks);
        FunctionAppWindowsRuntime.loadAllFunctionAppWindowsRuntimesFromMap(javaStacks);
    }

    private Map<String, Object> getRuntimesUsingHttpPipeline(AppServiceManager appServiceManager) {
        final HttpPipeline pipeline = appServiceManager.httpPipeline();
        final String apiVersion = appServiceManager.serviceClient().getApiVersion();
        final HttpRequest request = new HttpRequest(HttpMethod.GET, String.format("%s/providers/Microsoft.Web/functionAppStacks?api-version=%s", appServiceManager.serviceClient().getEndpoint(), apiVersion));
        try (final HttpResponse response = pipeline.send(request).block()) {
            if (Objects.nonNull(response) && response.getStatusCode() == 200) {
                final String responseBodyString = response.getBodyAsString().block();
                return mapper.readValue(responseBodyString, typeRef);
            }
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }
}
