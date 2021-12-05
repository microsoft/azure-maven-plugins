/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.ResourceNameAvailabilityInner;
import com.azure.resourcemanager.appservice.models.CheckNameResourceTypes;
import com.azure.resourcemanager.appservice.models.ResourceNameAvailabilityRequest;
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
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@Deprecated
public class AzureAppService extends SubscriptionScoped<AzureAppService> implements AzureService {

    public AzureAppService() { // for SPI
        super(AzureAppService::new);
    }

    private AzureAppService(@Nonnull final List<Subscription> subscriptions) {
        super(AzureAppService::new, subscriptions);
    }

    public FunctionApp functionApp(String id) {
        return Azure.az(AzureFunction.class).subscriptions(getSubscriptions()).get(id);
    }

    public FunctionApp functionApp(String resourceGroup, String name) {
        return Azure.az(AzureFunction.class).subscriptions(getSubscriptions()).get(resourceGroup, name);
    }

    public FunctionApp functionApp(String sid, String rg, String name) {
        return Azure.az(AzureFunction.class).subscriptions(getSubscriptions()).get(sid, rg, name);
    }

    public FunctionApp functionApp(FunctionAppEntity entity) {
        return StringUtils.isEmpty(entity.getId()) ?
                functionApp(entity.getSubscriptionId(), entity.getResourceGroup(), entity.getName()) : functionApp(entity.getId());
    }

    public List<FunctionApp> functionApps(boolean... force) {
        return Azure.az(AzureFunction.class).subscriptions(getSubscriptions()).list(force);
    }

    public WebApp webapp(String id) {
        return Azure.az(AzureWebApp.class).subscriptions(getSubscriptions()).get(id);
    }

    public WebApp webapp(String resourceGroup, String name) {
        return Azure.az(AzureWebApp.class).subscriptions(getSubscriptions()).get(resourceGroup, name);
    }

    public WebApp webapp(String sid, String rg, String name) {
        return Azure.az(AzureWebApp.class).subscriptions(getSubscriptions()).get(sid, rg, name);
    }

    public WebApp webapp(WebAppEntity webAppEntity) {
        return StringUtils.isEmpty(webAppEntity.getId()) ?
                webapp(webAppEntity.getSubscriptionId(), webAppEntity.getResourceGroup(), webAppEntity.getName()) : webapp(webAppEntity.getId());
    }

    public List<WebApp> webapps(boolean... force) {
        return Azure.az(AzureWebApp.class).subscriptions(getSubscriptions()).list(force);
    }

    @AzureOperation(name = "appservice.check_name.app", params = "name", type = AzureOperation.Type.SERVICE)
    public CheckNameAvailabilityResultEntity checkNameAvailability(String subscriptionId, String name) {
        final AppServiceManager azureResourceManager = getAppServiceManager(subscriptionId);
        final ResourceNameAvailabilityInner result = azureResourceManager.webApps().manager()
                .serviceClient().getResourceProviders().checkNameAvailability(new ResourceNameAvailabilityRequest()
                        .withName(name).withType(CheckNameResourceTypes.MICROSOFT_WEB_SITES));
        return new CheckNameAvailabilityResultEntity(result.nameAvailable(), result.reason().toString(), result.message());
    }

    public List<Runtime> listWebAppRuntimes(@Nonnull OperatingSystem os, @Nonnull JavaVersion version) {
        return Azure.az(AzureWebApp.class).listWebAppRuntimes(os, version);
    }

    public IAppServicePlan appServicePlan(String id) {
        return Azure.az(AzureAppServicePlan.class).subscriptions(getSubscriptions()).get(id);
    }

    public IAppServicePlan appServicePlan(String resourceGroup, String name) {
        return Azure.az(AzureAppServicePlan.class).subscriptions(getSubscriptions()).get(resourceGroup, name);
    }

    public IAppServicePlan appServicePlan(String sid, String rg, String name) {
        return Azure.az(AzureAppServicePlan.class).subscriptions(getSubscriptions()).get(sid, rg, name);
    }

    public IAppServicePlan appServicePlan(AppServicePlanEntity entity) {
        return StringUtils.isEmpty(entity.getId()) ?
                appServicePlan(entity.getSubscriptionId(), entity.getResourceGroup(), entity.getName()) : appServicePlan(entity.getId());
    }

    public List<IAppServicePlan> appServicePlans(boolean... force) {
        return Azure.az(AzureAppServicePlan.class).subscriptions(getSubscriptions()).list(force);
    }

    public List<IAppServicePlan> appServicePlans(String sid, boolean... force) {
        return Azure.az(AzureAppServicePlan.class).subscriptions(getSubscriptions()).list(sid, force);
    }

    public List<IAppServicePlan> appServicePlansByResourceGroup(String rg, boolean... force) {
        return ((AzureAppServicePlan) Azure.az(AzureAppServicePlan.class).subscriptions(getSubscriptions()))
                .appServicePlansByResourceGroup(rg, force);
    }

    @Deprecated
    @Cacheable(cacheName = "appservice/slot/{}", key = "$id")
    @AzureOperation(name = "appservice.get_deployment.id", params = "id", type = AzureOperation.Type.SERVICE)
    public WebAppDeploymentSlot deploymentSlot(String id) {
        return new WebAppDeploymentSlot(id, getAppServiceManager(Utils.getSubscriptionId(id)));
    }

    // todo: share codes with other library which leverage track2 mgmt sdk
    @Cacheable(cacheName = "appservice/{}/manager", key = "$sid")
    @AzureOperation(name = "appservice.get_client.subscription", params = "sid", type = AzureOperation.Type.SERVICE)
    public AppServiceManager getAppServiceManager(String sid) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, sid, account.getEnvironment());
        return AppServiceManager.configure()
                .withHttpClient(AzureService.getDefaultHttpClient())
                .withLogLevel(logLevel)
                .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
                .authenticate(account.getTokenCredential(sid), azureProfile);
    }

    private HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }

    public String name() {
        return "Microsoft.Web/sites";
    }
}
