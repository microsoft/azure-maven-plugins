/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

class AzureServicePrincipleAuthHelper {
    private static final String AZURE_CLI_GET_SUBSCRIPTION_FAIL = "Failed to get default subscription of Azure CLI, please login Azure CLI first.";
    private static final String AZURE_CLI_LOAD_TOKEN_FAIL = "Failed to load Azure CLI token file, please login Azure CLI first.";

    static AzureTokenCredentials getAzureServicePrincipleCredentials(AuthConfiguration config) throws InvalidConfigurationException, IOException {
        if (StringUtils.isBlank(config.getClient())) {
            throw new IllegalArgumentException("'Client Id' of your service principal is not configured.");
        }
        if (StringUtils.isBlank(config.getTenant())) {
            throw new IllegalArgumentException("'Tenant Id' of your service principal is not configured.");
        }
        final AzureEnvironment env = AzureAuthHelper.getAzureEnvironment(config.getEnvironment());
        if (StringUtils.isNotBlank(config.getCertificate())) {
            return new ApplicationTokenCredentials(config.getClient(), config.getTenant(),
                    FileUtils.readFileToByteArray(new File(config.getCertificate())), config.getCertificatePassword(), env);
        } else if (StringUtils.isNotBlank(config.getKey())) {
            return new ApplicationTokenCredentials(config.getClient(), config.getTenant(), config.getKey(), env);
        }
        throw new InvalidConfigurationException("Invalid auth configuration, either 'key' or 'certificate' is required.");

    }

    /**
     * Get Authenticated object from token file of Azure CLI 2.0 logged with Service Principal
     *
     * Note: This is a workaround for issue https://github.com/microsoft/azure-maven-plugins/issues/125
     *
     * @return Authenticated object if Azure CLI 2.0 is logged with Service Principal.
     * @throws InvalidConfigurationException where there are some configuration errors
     * @throws IOException                   where there read some read error when reading the file
     */
    static AzureTokenCredentials getCredentialFromAzureCliWithServicePrincipal() throws InvalidConfigurationException, IOException {
        final JsonObject subscription = getDefaultSubscriptionObject();
        final String servicePrincipalName = subscription == null ? null : subscription.get("user").getAsJsonObject().get("name").getAsString();
        if (servicePrincipalName == null) {
            throw new InvalidConfigurationException(AZURE_CLI_GET_SUBSCRIPTION_FAIL);
        }
        final JsonArray tokens = getAzureCliTokenList();
        if (tokens == null) {
            throw new InvalidConfigurationException(AZURE_CLI_LOAD_TOKEN_FAIL);
        }
        for (final JsonElement token : tokens) {
            final JsonObject tokenObject = (JsonObject) token;
            if (servicePrincipalName.equals(getStringFromJsonObject(tokenObject, "servicePrincipalId"))) {
                final String tenantId = getStringFromJsonObject(tokenObject, "servicePrincipalTenant");
                final String key = getStringFromJsonObject(tokenObject, "accessToken");
                final String certificateFile = getStringFromJsonObject(tokenObject, "certificateFile");
                final String env = getStringFromJsonObject(subscription, "environmentName");
                final String subscriptionId = getStringFromJsonObject(subscription, "id");
                if (StringUtils.isNotBlank(key)) {
                    return new ApplicationTokenCredentials(servicePrincipalName, tenantId, key, AzureAuthHelper.getAzureEnvironment(env))
                            .withDefaultSubscriptionId(subscriptionId);
                }
                if (StringUtils.isNotBlank(certificateFile) && new File(certificateFile).exists()) {
                    return new ApplicationTokenCredentials(servicePrincipalName, tenantId,
                        FileUtils.readFileToByteArray(new File(certificateFile)), null, AzureAuthHelper.getAzureEnvironment(env))
                            .withDefaultSubscriptionId(subscriptionId);
                }
            }
        }
        return null;
    }

    /**
     * Get default subscription object in azureProfile.json.
     *
     * @return default subscription, null if there is no default subscription.
     * @throws IOException when there are any read errors.
     */
    static JsonObject getDefaultSubscriptionObject() throws IOException {
        final File azureProfile = new File(AzureAuthHelper.getAzureConfigFolder(), Constants.AZURE_PROFILE_NAME);
        final String profileJsonContent = FileUtils.readFileToString(azureProfile, Constants.UTF8);
        final JsonArray subscriptionList = (new Gson()).fromJson(profileJsonContent, JsonObject.class).getAsJsonArray("subscriptions");
        for (final JsonElement child : subscriptionList) {
            final JsonObject subscription = (JsonObject) child;
            if (subscription.getAsJsonPrimitive("isDefault").getAsBoolean()) {
                return subscription;
            }
        }
        return null;
    }

    /**
     * Get token array in accessTokens.json.
     *
     * @return the token array.
     * @throws IOException when there are any read errors.
     */
    static JsonArray getAzureCliTokenList() throws IOException {
        final File azureTokenFile = new File(AzureAuthHelper.getAzureConfigFolder(), Constants.AZURE_TOKEN_NAME);
        final String tokenJsonContent = FileUtils.readFileToString(azureTokenFile, Constants.UTF8);
        return (new Gson()).fromJson(tokenJsonContent, JsonArray.class);
    }

    private static String getStringFromJsonObject(JsonObject obj, String property) {
        if (obj == null || property == null || !obj.has(property)) {
            return null;
        }
        return obj.get(property).getAsString();
    }

    private AzureServicePrincipleAuthHelper() {

    }
}
