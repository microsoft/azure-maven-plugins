/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppClusterResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.Microservices4SpringManager;
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.rest.LogLevel;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.plexus.util.StringUtils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class SpringServiceClient {

    protected static final String NO_CLUSTER = "No cluster named %s found in subscription %s";

    private String subscriptionId;
    private Microservices4SpringManager springManager;

    public SpringServiceClient(AzureTokenCredentials azureTokenCredentials, String subscriptionId) {
        this(azureTokenCredentials, subscriptionId, LogLevel.NONE);
    }

    public SpringServiceClient(AzureTokenCredentials azureTokenCredentials, String subscriptionId, LogLevel logLevel) {
        subscriptionId = StringUtils.isEmpty(subscriptionId) ? azureTokenCredentials.defaultSubscriptionId() : subscriptionId;
        this.subscriptionId = subscriptionId;
        this.springManager = Microservices4SpringManager.configure()
                .withLogLevel(logLevel)
                .authenticate(azureTokenCredentials, subscriptionId);
    }

    public SpringDeploymentClient newSpringDeploymentClient(String subscriptionId, String cluster, String app, String deployment) {
        final SpringDeploymentClient.Builder builder = new SpringDeploymentClient.Builder();
        return builder.withSubscriptionId(subscriptionId)
                .withSpringServiceClient(this)
                .withClusterName(cluster)
                .withAppName(app)
                .withDeploymentName(deployment)
                .build();
    }

    public SpringAppClient newSpringAppClient(String subscriptionId, String cluster, String app) {
        final SpringAppClient.Builder builder = new SpringAppClient.Builder();
        return builder.withSubscriptionId(subscriptionId)
                .withSpringServiceClient(this)
                .withClusterName(cluster)
                .withAppName(app)
                .build();
    }

    public SpringAppClient newSpringAppClient(SpringConfiguration configuration) {
        return newSpringAppClient(configuration.getSubscriptionId(), configuration.getClusterName(), configuration.getAppName());
    }

    public List<AppClusterResourceInner> getAvailableClusters() {
        final PagedList<AppClusterResourceInner> clusterList = getSpringManager().appClusters()
                .inner().list();
        clusterList.loadAll();
        return new ArrayList<>(clusterList);
    }

    public AppClusterResourceInner getClusterByName(String cluster) {
        final List<AppClusterResourceInner> clusterList = getAvailableClusters();
        return clusterList.stream().filter(appClusterResourceInner -> appClusterResourceInner.name().equals(cluster))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException(String.format(NO_CLUSTER, cluster, subscriptionId)));
    }

    public String getResourceGroupByCluster(String clusterName) {
        final AppClusterResourceInner cluster = getClusterByName(clusterName);
        final String[] attributes = cluster.id().split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Microservices4SpringManager getSpringManager() {
        return springManager;
    }
}
