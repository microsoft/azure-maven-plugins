/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.applicationinsights;

import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import org.apache.commons.codec.binary.StringUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationInsightsManager {

    private ApplicationInsightsManagementClient aiClient;
    private static final Logger LOGGER = Logger.getLogger("class com.microsoft.applicationinsights.management");

    public ApplicationInsightsManager(AzureTokenCredentials tokenCredentials, String userAgent) throws AzureExecutionException {
        try {
            final String tenantId = tokenCredentials.domain();
            final String token = tokenCredentials.getToken(tokenCredentials.environment().resourceManagerEndpoint());
            this.aiClient = new ApplicationInsightsManagementClient(tenantId, token, userAgent);
        } catch (IOException | RestOperationException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }

    public Resource getApplicationInsightInstance(String subscriptionId, String resourceGroup, String name) throws AzureExecutionException {
        final Level logLevel = LOGGER.getLevel();
        try {
            LOGGER.setLevel(Level.OFF);
            return aiClient.getResources(subscriptionId)
                    .stream()
                    .filter(resource -> StringUtils.equals(resource.getResourceGroup(), resourceGroup) &&
                            StringUtils.equals(resource.getName(), name))
                    .findFirst()
                    .orElse(null);
        } catch (IOException | RestOperationException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        } finally {
            LOGGER.setLevel(logLevel);
        }
    }

    public Resource createApplicationInsight(String subscriptionId, String resourceGroup, String name, String location) throws AzureExecutionException {
        final Level logLevel = LOGGER.getLevel();
        try {
            LOGGER.setLevel(Level.OFF);
            if (!isResourceGroupExists(subscriptionId, resourceGroup)) {
                aiClient.createResourceGroup(subscriptionId, resourceGroup, location);
            }
            return aiClient.createResource(subscriptionId, resourceGroup, name, location);
        } catch (IOException | RestOperationException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        } finally {
            LOGGER.setLevel(logLevel);
        }
    }

    private boolean isResourceGroupExists(String subscriptionId, String resourceGroupName) throws IOException, RestOperationException {
        return aiClient.getResourceGroups(subscriptionId).stream()
                .anyMatch(resourceGroup -> StringUtils.equals(resourceGroup.getName(), resourceGroupName));
    }
}
