/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.auth.AzureResourceManagerFactory;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class AzureAppService extends SubscriptionScoped<AzureAppService> implements AzureService {

    public AzureAppService() { // for SPI
        super(AzureAppService::new);
    }

    private AzureAppService(@Nonnull final List<Subscription> subscriptions) {
        super(AzureAppService::new, subscriptions);
    }

    public IWebApp webapp(String id) {
        final WebAppEntity webAppEntity = WebAppEntity.builder().id(id).build();
        return webapp(webAppEntity);
    }

    public IWebApp webapp(String resourceGroup, String name) {
        return webapp(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public IWebApp webapp(String subscriptionId, String resourceGroup, String name) {
        final WebAppEntity webAppEntity = WebAppEntity.builder().subscriptionId(subscriptionId).resourceGroup(resourceGroup).name(name).build();
        return webapp(webAppEntity);
    }

    public IWebApp webapp(WebAppEntity webAppEntity) {
        final String subscriptionId = getSubscriptionFromResourceEntity(webAppEntity);
        return new WebApp(webAppEntity, AzureResourceManagerFactory.produce(subscriptionId));
    }

    public List<IWebApp> webapps() {
        return getSubscriptions().stream()
                .map(subscription -> AzureResourceManagerFactory.produce(subscription.getId()))
                .flatMap(azureResourceManager -> azureResourceManager.webApps().list().stream())
                .collect(Collectors.toList()).stream()
                .filter(webAppBasic -> !StringUtils.containsIgnoreCase(webAppBasic.innerModel().kind(), "functionapp")) // Filter out function apps
                .map(webAppBasic -> webapp(webAppBasic.id()))
                .collect(Collectors.toList());
    }

    public IAppServicePlan appServicePlan(String id) {
        final AppServicePlanEntity appServicePlanEntity = AppServicePlanEntity.builder().id(id).build();
        return appServicePlan(appServicePlanEntity);
    }

    public IAppServicePlan appServicePlan(String resourceGroup, String name) {
        return appServicePlan(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public IAppServicePlan appServicePlan(String subscriptionId, String resourceGroup, String name) {
        final AppServicePlanEntity appServicePlanEntity = AppServicePlanEntity.builder()
                .subscriptionId(subscriptionId)
                .resourceGroup(resourceGroup)
                .name(name).build();
        return appServicePlan(appServicePlanEntity);
    }

    public IAppServicePlan appServicePlan(AppServicePlanEntity appServicePlanEntity) {
        final String subscriptionId = getSubscriptionFromResourceEntity(appServicePlanEntity);
        return new AppServicePlan(appServicePlanEntity, AzureResourceManagerFactory.produce(subscriptionId));
    }

    public List<IAppServicePlan> appServicePlans() {
        return getSubscriptions().stream()
                .map(subscription -> AzureResourceManagerFactory.produce(subscription.getId()))
                .flatMap(azureResourceManager -> azureResourceManager.appServicePlans().list().stream())
                .collect(Collectors.toList()).stream()
                .map(appServicePlan -> appServicePlan(appServicePlan.id()))
                .collect(Collectors.toList());
    }

    public IWebAppDeploymentSlot deploymentSlot(String id) {
        return deploymentSlot(WebAppDeploymentSlotEntity.builder().id(id).build());
    }

    public IWebAppDeploymentSlot deploymentSlot(String resourceGroup, String appName, String slotName) {
        return deploymentSlot(getDefaultSubscription().getId(), resourceGroup, appName, slotName);
    }

    public IWebAppDeploymentSlot deploymentSlot(String subscriptionId, String resourceGroup, String appName, String slotName) {
        return deploymentSlot(WebAppDeploymentSlotEntity.builder().subscriptionId(subscriptionId).resourceGroup(resourceGroup).webappName(appName).name(slotName).build());
    }

    public IWebAppDeploymentSlot deploymentSlot(WebAppDeploymentSlotEntity deploymentSlot) {
        final String subscriptionId = getSubscriptionFromResourceEntity(deploymentSlot);
        return new WebAppDeploymentSlot(deploymentSlot, AzureResourceManagerFactory.produce(subscriptionId));
    }

    private String getSubscriptionFromResourceEntity(@Nonnull IAzureResourceEntity resourceEntity) {
        if (StringUtils.isNotEmpty(resourceEntity.getId())) {
            return Utils.getSubscriptionId(resourceEntity.getId());
        }
        if (StringUtils.isNotEmpty(resourceEntity.getSubscriptionId())) {
            return resourceEntity.getSubscriptionId();
        }
        throw new AzureToolkitRuntimeException("Subscription id is required for this request.");
    }
}
