/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppClusterResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.Microservices4SpringManager;
import com.microsoft.azure.maven.spring.SpringConfiguration;
import com.microsoft.rest.LogLevel;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpringServiceUtils {

    private SpringServiceUtils() {

    }

    private static LogLevel logLevel = LogLevel.NONE;
    private static Microservices4SpringManager springManager;
    protected static final String NO_CLUSTER = "No cluster named %s found in subscription %s";

    public static Microservices4SpringManager getSpringManager(String subscriptionId) {
        if (springManager == null || !springManager.subscriptionId().equals(subscriptionId)) {
            synchronized (SpringServiceUtils.class) {
                if (springManager == null || !springManager.subscriptionId().equals(subscriptionId)) {
                    springManager = Microservices4SpringManager.configure()
                            .withLogLevel(logLevel)
                            .authenticate(getCredential(), subscriptionId);
                }
            }
        }
        return springManager;
    }

    public static AzureTokenCredentials getCredential() {
        final AzureEnvironment mockEnvironment = new AzureEnvironment(new HashMap<String, String>() {
            {
                put(AzureEnvironment.Endpoint.RESOURCE_MANAGER.toString(), "http://localhost:8080/swagger-jaxrs-server-1.0.0/");
            }
        });

        return new AzureTokenCredentials(mockEnvironment, null) {
            @Override
            public String getToken(String resource) throws IOException {
                return "Token";
            }
        };
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

    public static List<AppClusterResourceInner> getAvailableClusters(String subscriptionId) {
        final Microservices4SpringManager microservices4SpringManager = getSpringManager(subscriptionId);
        final PagedList<AppClusterResourceInner> clusterList = microservices4SpringManager.appClusters()
                .inner().list();
        clusterList.loadAll();
        return new ArrayList<>(clusterList);
    }

    public static AppClusterResourceInner getClusterByName(String cluster, String subscriptionId) {
        final List<AppClusterResourceInner> clusterList = getAvailableClusters(subscriptionId);
        return clusterList.stream().filter(appClusterResourceInner -> appClusterResourceInner.name().equals(cluster))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException(String.format(NO_CLUSTER, cluster, subscriptionId)));
    }

    public static String getResourceGroupByCluster(String clusterName, String subscriptionId) {
        final AppClusterResourceInner cluster = getClusterByName(clusterName, subscriptionId);
        final String[] attributes = cluster.id().split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    public static void setLogLevel(LogLevel logLevel) {
        SpringServiceUtils.logLevel = logLevel;
    }
}
