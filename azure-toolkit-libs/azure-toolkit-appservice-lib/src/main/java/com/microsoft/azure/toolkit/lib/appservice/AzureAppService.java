/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAppService extends SubscriptionScoped<AzureAppService> implements AzureService {

    public AzureAppService() { // for SPI
        super(AzureAppService::new);
    }

    private AzureAppService(@Nonnull final List<Subscription> subscriptions) {
        super(AzureAppService::new, subscriptions);
    }

    @Cacheable(cacheName = "appservcie/functionapp/{}", key = "$id")
    @AzureOperation(name = "functionapp.get.id", params = {"id"}, type = AzureOperation.Type.SERVICE)
    public IFunctionApp functionApp(String id) {
        return new FunctionApp(id, getAzureResourceManager(Utils.getSubscriptionId(id)));
    }

    public IFunctionApp functionApp(String resourceGroup, String name) {
        return functionApp(getDefaultSubscription().getId(), resourceGroup, name);
    }

    @Cacheable(cacheName = "appservcie/{}/rg/{}/functionapp/{}", key = "$sid/$rg/$name")
    @AzureOperation(name = "functionapp.get.name|rg|sid", params = {"name", "rg", "sid"}, type = AzureOperation.Type.SERVICE)
    public IFunctionApp functionApp(String sid, String rg, String name) {
        return new FunctionApp(sid, rg, name, getAzureResourceManager(sid));
    }

    public IFunctionApp functionApp(FunctionAppEntity entity) {
        return StringUtils.isEmpty(entity.getId()) ?
                functionApp(entity.getSubscriptionId(), entity.getResourceGroup(), entity.getId()) : functionApp(entity.getId());
    }

    @Preload
    @AzureOperation(name = "functionapp.list.subscription|selected", type = AzureOperation.Type.SERVICE)
    public List<IFunctionApp> functionApps(boolean... force) {
        return getSubscriptions().stream().parallel()
                .flatMap(subscription -> functionApps(subscription.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservcie/{}/functionapps", key = "$sid", condition = "!(force&&force[0])")
    @AzureOperation(name = "functionapp.list.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    private List<IFunctionApp> functionApps(String sid, boolean... force) {
        final AzureResourceManager azureResourceManager = getAzureResourceManager(sid);
        return azureResourceManager
                .functionApps().list().stream().parallel()
                .filter(functionAppBasic -> StringUtils.containsIgnoreCase(functionAppBasic.innerModel().kind(), "functionapp")) // Filter out function apps
                .map(functionAppBasic -> new FunctionApp(functionAppBasic, azureResourceManager))
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservcie/webapp/{}", key = "$id")
    @AzureOperation(name = "webapp.get.id", params = "id", type = AzureOperation.Type.SERVICE)
    public IWebApp webapp(String id) {
        return new WebApp(id, getAzureResourceManager(Utils.getSubscriptionId(id)));
    }

    public IWebApp webapp(String resourceGroup, String name) {
        return webapp(getDefaultSubscription().getId(), resourceGroup, name);
    }

    @Cacheable(cacheName = "appservcie/{}/rg/{}/webapp/{}", key = "$sid/$rg/$name")
    @AzureOperation(name = "webapp.get.name|rg|sid", params = {"name", "rg", "sid"}, type = AzureOperation.Type.SERVICE)
    public IWebApp webapp(String sid, String rg, String name) {
        return new WebApp(sid, rg, name, getAzureResourceManager(sid));
    }

    public IWebApp webapp(WebAppEntity webAppEntity) {
        return StringUtils.isEmpty(webAppEntity.getId()) ?
                webapp(webAppEntity.getSubscriptionId(), webAppEntity.getResourceGroup(), webAppEntity.getId()) : webapp(webAppEntity.getId());
    }

    @Preload
    @AzureOperation(name = "webapp.list.subscription|selected", type = AzureOperation.Type.SERVICE)
    public List<IWebApp> webapps(boolean... force) {
        return getSubscriptions().stream().parallel()
                .flatMap(subscription -> webapps(subscription.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservcie/{}/webapps", key = "$sid", condition = "!(force&&force[0])")
    @AzureOperation(name = "webapp.list.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    private List<IWebApp> webapps(String sid, boolean... force) {
        final AzureResourceManager azureResourceManager = getAzureResourceManager(sid);
        return azureResourceManager.webApps().list().stream().parallel()
                .filter(webAppBasic -> !StringUtils.containsIgnoreCase(webAppBasic.innerModel().kind(), "functionapp")) // Filter out function apps
                .map(webAppBasic -> new WebApp(webAppBasic, azureResourceManager))
                .collect(Collectors.toList());
    }

    public @Nonnull
    @AzureOperation(name = "webapp|runtime.list.os|version", params = {"os.getValue()", "version.getValue()"}, type = AzureOperation.Type.SERVICE)
    List<Runtime> listWebAppRuntimes(@Nonnull OperatingSystem os, @Nonnull JavaVersion version) {
        return Runtime.WEBAPP_RUNTIME.stream()
                .filter(runtime -> Objects.equals(os, runtime.getOperatingSystem()) && Objects.equals(version, runtime.getJavaVersion()))
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservcie/plan/{}", key = "$id")
    @AzureOperation(name = "appservice|plan.get.id", params = "id", type = AzureOperation.Type.SERVICE)
    public IAppServicePlan appServicePlan(String id) {
        return new AppServicePlan(id, getAzureResourceManager(Utils.getSubscriptionId(id)));
    }

    public IAppServicePlan appServicePlan(String resourceGroup, String name) {
        return appServicePlan(getDefaultSubscription().getId(), resourceGroup, name);
    }

    @Cacheable(cacheName = "appservcie/{}/rg/{}/plan/{}", key = "$sid/$rg/$name")
    @AzureOperation(name = "appservice|plan.get.name|rg|sid", params = {"name", "rg", "sid"}, type = AzureOperation.Type.SERVICE)
    public IAppServicePlan appServicePlan(String sid, String rg, String name) {
        return new AppServicePlan(sid, rg, name, getAzureResourceManager(sid));
    }

    public IAppServicePlan appServicePlan(AppServicePlanEntity entity) {
        return StringUtils.isEmpty(entity.getId()) ?
                appServicePlan(entity.getSubscriptionId(), entity.getResourceGroup(), entity.getId()) : appServicePlan(entity.getId());
    }

    @Preload
    @AzureOperation(name = "appservice|plan.list.subscription|selected", type = AzureOperation.Type.SERVICE)
    public List<IAppServicePlan> appServicePlans(boolean... force) {
        return getSubscriptions().stream().parallel()
                .flatMap(subscription -> appServicePlans(subscription.getId(), force).stream())
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservcie/{}/plans", key = "$sid", condition = "!(force&&force[0])")
    @AzureOperation(name = "appservice|plan.list.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    public List<IAppServicePlan> appServicePlans(String sid, boolean... force) {
        final AzureResourceManager azureResourceManager = getAzureResourceManager(sid);
        return azureResourceManager.appServicePlans().list().stream().parallel()
                .map(appServicePlan -> new AppServicePlan(appServicePlan, azureResourceManager))
                .collect(Collectors.toList());
    }

    @Cacheable(cacheName = "appservcie/rg/{}/plans", key = "$rg", condition = "!(force&&force[0])")
    @AzureOperation(name = "appservice|plan.list.rg", params = "rg", type = AzureOperation.Type.SERVICE)
    public List<IAppServicePlan> appServicePlansByResourceGroup(String rg, boolean... force) {
        return getSubscriptions().stream().parallel()
                .map(subscription -> getAzureResourceManager(subscription.getId()))
                .flatMap(azureResourceManager -> azureResourceManager.appServicePlans().listByResourceGroup(rg).stream()
                        .map(appServicePlan -> new AppServicePlan(appServicePlan, azureResourceManager)))
                .collect(Collectors.toList());
    }

    @Deprecated
    @Cacheable(cacheName = "appservcie/slot/{}", key = "$id")
    @AzureOperation(name = "appservice|deployment.get.id", params = "id", type = AzureOperation.Type.SERVICE)
    public IWebAppDeploymentSlot deploymentSlot(String id) {
        return new WebAppDeploymentSlot(id, getAzureResourceManager(Utils.getSubscriptionId(id)));
    }

    // todo: share codes with other library which leverage track2 mgmt sdk
    @Cacheable(cacheName = "appservice/{}/manager", key = "$sid")
    @AzureOperation(name = "appservice.get_client.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    public AzureResourceManager getAzureResourceManager(String sid) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(account.getEnvironment());
        return AzureResourceManager.configure()
                .withLogLevel(logLevel)
                .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
                .authenticate(account.getTokenCredential(sid), azureProfile)
                .withSubscription(sid);
    }

    private HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
