/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppClusterResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.Microservices4SpringManager;
import com.microsoft.azure.maven.spring.SpringConfiguration;
import com.microsoft.rest.LogLevel;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class SpringServiceUtils {

    protected static final String NO_CLUSTER = "No cluster named %s found in subscription %s";

    private static Microservices4SpringManager springManager;
    private static String subscriptionId;

    public static Microservices4SpringManager setSpringManager(AuthConfiguration authConfiguration, String subscriptionId)
            throws InvalidConfigurationException, IOException {
        return setSpringManager(authConfiguration, subscriptionId, LogLevel.NONE);
    }

    public static Microservices4SpringManager setSpringManager(AuthConfiguration authConfiguration, String subscriptionId, LogLevel logLevel)
            throws InvalidConfigurationException, IOException {
        SpringServiceUtils.subscriptionId = subscriptionId;
        if (springManager == null || !springManager.subscriptionId().equals(subscriptionId)) {
            synchronized (SpringServiceUtils.class) {
                if (springManager == null || !springManager.subscriptionId().equals(subscriptionId)) {
                    final AzureTokenCredentials credentials = AzureAuthHelper.getAzureTokenCredentials(authConfiguration);
                    final String authSubscription = StringUtils.isEmpty(subscriptionId) ? credentials.defaultSubscriptionId() : subscriptionId;
                    springManager = Microservices4SpringManager.configure()
                            .withLogLevel(logLevel)
                            .authenticate(credentials, authSubscription);
                }
            }
        }
        return springManager;
    }

    public static SpringDeploymentClient newSpringDeploymentClient(String subscriptionId, String cluster, String app, String deployment) {
        final SpringDeploymentClient.Builder builder = new SpringDeploymentClient.Builder();
        return builder.withSubscriptionId(subscriptionId)
                .withClusterName(cluster)
                .withAppName(app)
                .withDeploymentName(deployment)
                .build();
    }

    public static SpringAppClient newSpringAppClient(String subscriptionId, String cluster, String app) {
        final SpringAppClient.Builder builder = new SpringAppClient.Builder();
        return builder.withSubscriptionId(subscriptionId)
                .withClusterName(cluster)
                .withAppName(app)
                .build();
    }

    public static SpringAppClient newSpringAppClient(SpringConfiguration configuration) {
        return newSpringAppClient(configuration.getSubscriptionId(), configuration.getClusterName(), configuration.getAppName());
    }

    public static List<AppClusterResourceInner> getAvailableClusters() {
        final PagedList<AppClusterResourceInner> clusterList = getSpringManager().appClusters()
                .inner().list();
        clusterList.loadAll();
        return new ArrayList<>(clusterList);
    }

    public static AppClusterResourceInner getClusterByName(String cluster) {
        final List<AppClusterResourceInner> clusterList = getAvailableClusters();
        return clusterList.stream().filter(appClusterResourceInner -> appClusterResourceInner.name().equals(cluster))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException(String.format(NO_CLUSTER, cluster, subscriptionId)));
    }

    public static String getResourceGroupByCluster(String clusterName) {
        final AppClusterResourceInner cluster = getClusterByName(clusterName);
        final String[] attributes = cluster.id().split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    public static Microservices4SpringManager getSpringManager() {
        if (springManager == null) {
            throw new RuntimeException("Should invoke `setSpringManager` first");
        }
        return springManager;
    }

    private SpringServiceUtils() {

    }
}
