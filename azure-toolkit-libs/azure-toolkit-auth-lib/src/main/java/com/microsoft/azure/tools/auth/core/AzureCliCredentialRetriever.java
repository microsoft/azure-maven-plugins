/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCliAccountProfile;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.common.exception.CommandExecuteException;
import com.microsoft.azure.tools.common.util.CommandUtil;
import com.microsoft.azure.tools.common.util.JsonUtils;

import java.io.IOException;

public class AzureCliCredentialRetriever extends AbstractCredentialRetriever {
    private static final String CLOUD_SHELL_ENV_KEY = "ACC_CLOUD";

    public AzureCredentialWrapper retrieve() throws LoginFailureException {
        AzureCliAccountProfile accountInfo = getProfile();

        if (accountInfo == null) {
            throw new LoginFailureException("Please run `az login` to login your Azure Cli.");
        }
        AzureEnvironment envFromCli = AuthHelper.parseAzureEnvironment(accountInfo.getEnvironment());
        AzureCliCredential cliCredential = new AzureCliCredentialBuilder().build();
        AzureCredentialWrapper credentialWrapper = new AzureCredentialWrapper(
                isInCloudShell() ? AuthMethod.CLOUD_SHELL : AuthMethod.AZURE_CLI, cliCredential, envFromCli);
        credentialWrapper.setDefaultSubscriptionId(accountInfo.getSubscriptionId());
        credentialWrapper.setTenantId(accountInfo.getTenantId());
        return credentialWrapper;
    }

    private static AzureCliAccountProfile getProfile() {
        final String accountInfo;
        try {
            accountInfo = CommandUtil.executeCommandAndGetOutput("az account show", null);
            final JsonObject accountObject = JsonUtils.getGson().fromJson(accountInfo, JsonObject.class);
            String tenantId = accountObject.get("tenantId").getAsString();
            String environment = accountObject.get("environmentName").getAsString();
            String subscriptionId = accountObject.get("id").getAsString();
            String userName = accountObject.get("user").getAsJsonObject().get("name").getAsString();
            String userType = accountObject.get("user").getAsJsonObject().get("type").getAsString();
            return new AzureCliAccountProfile(tenantId, environment, userName, userType, subscriptionId);
        } catch (InterruptedException | IOException | JsonParseException | CommandExecuteException e) {
            // Return null when azure cli is not signed in
            return null;
        }
    }

    private static boolean isInCloudShell() {
        return System.getenv(CLOUD_SHELL_ENV_KEY) != null;
    }
}
