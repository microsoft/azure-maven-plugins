/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.azurecli;

import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.exception.CommandExecuteException;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;

public class AzureCliCredentialRetriever extends AbstractCredentialRetriever {
    private static final String CLOUD_SHELL_ENV_KEY = "ACC_CLOUD";

    public AzureCliCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        AzureCliAccountProfile accountInfo = getProfile();
        checkAzureEnvironmentConflict(env, AzureEnvironmentUtils.stringToAzureEnvironment(accountInfo.getEnvironment()));
        AzureCliCredential cliCredential = new AzureCliCredentialBuilder().build();
        validateTokenCredential(cliCredential);
        return new AzureCredentialWrapper(isInCloudShell() ? AuthMethod.CLOUD_SHELL : AuthMethod.AZURE_CLI, cliCredential, getAzureEnvironment())
            .withDefaultSubscriptionId(accountInfo.getSubscriptionId())
            .withTenantId(accountInfo.getTenantId());
    }

    private static void checkAzureEnvironmentConflict(AzureEnvironment env, AzureEnvironment envCli) throws LoginFailureException {
        if (env != null && envCli != null && !Objects.equals(env, envCli)) {
            throw new LoginFailureException(String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                    "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                AzureEnvironmentUtils.azureEnvironmentToString(envCli),
                AzureEnvironmentUtils.getCloudNameForAzureCli(env)));
        }
    }

    private static AzureCliAccountProfile getProfile() throws LoginFailureException {
        final String accountInfo;
        try {
            accountInfo = Utils.executeCommandAndGetOutput("az account show", null);
            final JsonObject accountObject = JsonUtils.getGson().fromJson(accountInfo, JsonObject.class);
            String tenantId = accountObject.get("tenantId").getAsString();
            String environment = accountObject.get("environmentName").getAsString();
            String subscriptionId = accountObject.get("id").getAsString();
            String userName = accountObject.get("user").getAsJsonObject().get("name").getAsString();
            String userType = accountObject.get("user").getAsJsonObject().get("type").getAsString();
            return new AzureCliAccountProfile(tenantId, environment, userName, userType, subscriptionId);
        } catch (InterruptedException | IOException | CommandExecuteException | JsonParseException | NullPointerException ex) {
            throw new LoginFailureException(String.format("Cannot get account info from azure cli through `az account show`, " +
                "please run `az login` to login your Azure Cli, detailed error: %s", ex.getMessage()));
        }
    }

    private static boolean isInCloudShell() {
        return StringUtils.isNotBlank(System.getenv(CLOUD_SHELL_ENV_KEY));
    }
}
