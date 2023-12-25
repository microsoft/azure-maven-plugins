/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.WebAppStackInner;
import com.azure.resourcemanager.appservice.models.WebAppMajorVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppWindowsRuntime;
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

public class WebAppServiceSubscription extends AppServiceServiceSubscription {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
    };

    protected WebAppServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureAppService service) {
        super(subscriptionId, service);
    }

    protected WebAppServiceSubscription(@Nonnull AppServiceManager remote, @Nonnull AzureAppService service) {
        super(remote, service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.getWebAppModule());
    }

    @Nonnull
    public List<WebAppRuntime> listWebAppMajorRuntimes() {
        if (!WebAppWindowsRuntime.isLoaded() && !WebAppLinuxRuntime.isLoaded()) {
            loadRuntimes();
        }
        return Stream.concat(WebAppWindowsRuntime.getMajorRuntimes().stream(), WebAppLinuxRuntime.getMajorRuntimes().stream()).collect(Collectors.toList());
    }

    public synchronized void loadRuntimes() {
        loadRuntimesUsingHttpPipeline();
    }

    public synchronized void loadRuntimesUsingSdk() {
        final AppServiceManager remote = this.getRemote();
        if (Objects.isNull(remote)) {
            return;
        }
        if (WebAppWindowsRuntime.isLoaded() && WebAppLinuxRuntime.isLoaded()) {
            return;
        }

        final Map<String, WebAppStackInner> stacks = remote.serviceClient().getProviders()
            .getWebAppStacksAsync().toStream()
            .filter(stack -> StringUtils.equalsAnyIgnoreCase(stack.name(), "javacontainers", "java"))
            .collect(Collectors.toMap(s -> s.name().toLowerCase(), s -> s));

        final List<WebAppMajorVersion> javaStacks = stacks.get("java").majorVersions();
        final List<WebAppMajorVersion> containerStacks = stacks.get("javacontainers").majorVersions();

        // fill `Runtime` only with major versions
        WebAppLinuxRuntime.loadAllWebAppLinuxRuntimes(javaStacks, containerStacks);
        WebAppWindowsRuntime.loadAllWebAppWindowsRuntimes(javaStacks, containerStacks);
    }

    @SuppressWarnings("DataFlowIssue")
    public synchronized void loadRuntimesUsingHttpPipeline() {
        final AppServiceManager remote = this.getRemote();
        if (Objects.isNull(remote)) {
            return;
        }
        if (WebAppWindowsRuntime.isLoaded() && WebAppLinuxRuntime.isLoaded()) {
            return;
        }

        final Map<String, Object> result = getRuntimesUsingHttpPipeline(remote);
        if (result.isEmpty()) {
            return;
        }
        final List<Map<String, Object>> stacksList = Utils.get(result, "$.value");
        final Map<String, Object> stacks = stacksList.stream().filter(s -> StringUtils.equalsAnyIgnoreCase((CharSequence) s.get("name"), "javacontainers", "java"))
            .collect(Collectors.toMap(s -> ((String) s.get("name")).toLowerCase(), s -> s));
        final List<Map<String, Object>> javaStacks = Utils.get(stacks, "$.java.properties.majorVersions");
        final List<Map<String, Object>> containerStacks = Utils.get(stacks, "$.javacontainers.properties.majorVersions");
        // fill `Runtime` only with major versions
        WebAppLinuxRuntime.loadAllWebAppLinuxRuntimesFromMap(javaStacks, containerStacks);
        WebAppWindowsRuntime.loadAllWebAppWindowsRuntimesFromMap(javaStacks, containerStacks);
    }

    private Map<String, Object> getRuntimesUsingHttpPipeline(AppServiceManager appServiceManager) {
        final HttpPipeline pipeline = appServiceManager.httpPipeline();
        final String apiVersion = appServiceManager.serviceClient().getApiVersion();
        final HttpRequest request = new HttpRequest(HttpMethod.GET, String.format("%s/providers/Microsoft.Web/webAppStacks?api-version=%s", appServiceManager.serviceClient().getEndpoint(), apiVersion));
        try (HttpResponse response = pipeline.send(request).block()) {
            if (Objects.nonNull(response) && response.getStatusCode() == 200) {
                final String responseBodyString = response.getBodyAsString().block();
                return mapper.readValue(responseBodyString, typeRef);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }
}
