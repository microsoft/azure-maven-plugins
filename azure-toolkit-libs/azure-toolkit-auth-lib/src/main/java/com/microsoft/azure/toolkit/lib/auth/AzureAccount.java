/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.core.oauth.OAuthAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAccount implements AzureService {
    private Account account;

    public static void main(String[] args) throws LoginFailureException {
        AzureAccount az = Azure.az(AzureAccount.class);
        for (Account account : az.accounts()) {
            System.out.println("Got: " + account.getMethod());
            System.out.println("Got: " + account.isAvailable());
            System.out.println("Got: " + account.isAuthenticated());
            az.login(account);
        }

    }

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
                .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not initialized."));
    }

    public List<Account> accounts() {
        return buildAccountMap(AzureEnvironment.AZURE).values().stream().collect(Collectors.toList());
    }

    public AzureAccount login(@Nonnull Account targetAccount) throws LoginFailureException {
        account = targetAccount;
        account.authenticate();
        return this;
    }

    public AzureAccount login(AuthType type) {
        AuthConfiguration auth = new AuthConfiguration();
        auth.setType(type);
        loginWithAuthConfiguration(auth);
        return this;
    }

    public Account servicePrincipalAccount(AuthConfiguration config) {
        return null;
    }

    public void login(@Nonnull AuthConfiguration auth) throws LoginFailureException {
        // update the env state of AzureAccount when auth configuration has a strong configuration of env
        AzureEnvironment environment = ObjectUtils.firstNonNull(auth.getEnvironment(), AzureEnvironment.AZURE);
        Objects.requireNonNull(auth, "Null auth configuration is illegal for login.");
        AzureEnvironmentUtils.setupAzureEnvironment(auth.getEnvironment());
        loginWithAuthConfiguration(auth);
        if (auth.getEnvironment() != null && this.account.getEnvironment() != null
                && this.account.getEnvironment() != auth.getEnvironment()
                && this.account.isAuthenticated()) {

            String expectedEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(auth.getEnvironment());
            String realEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(this.account.getEnvironment());

            // conflicting configuration of azure environment
            switch (this.account.getMethod()) {
                case AZURE_CLI:
                    throw new LoginFailureException(String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                                    "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                            realEnv,
                            expectedEnv));

                case AZURE_SECRET_FILE:
                    throw new LoginFailureException(String.format("The azure cloud from maven login '%s' doesn't match with your auth configuration, " +
                                    "please switch to other auth method for '%s' environment.",
                            realEnv,
                            expectedEnv));
                case VSCODE:
                    throw new LoginFailureException(String.format("The azure cloud from vscode '%s' doesn't match with your auth configuration: %s, " +
                                    "you can change it by pressing F1 in VSCode and find \">azure: sign in to Azure Cloud\" command " +
                                    "to change azure cloud in vscode.",
                            realEnv,
                            expectedEnv));
                default:
                    // empty
            }
        }
    }

    private void loginWithAuthConfiguration(@Nonnull AuthConfiguration auth) {
        // need to be implemented
    }

    private static Map<AuthType, Account> buildAccountMap(AzureEnvironment env) {
        Map<AuthType, Account> map = new LinkedHashMap<>();
        // SP is not there since it requires special constructor argument and it is handled in login(AuthConfiguration auth)
        AzureEnvironment environmentOrDefault = ObjectUtils.firstNonNull(env, AzureEnvironment.AZURE);
        // map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityAccountEntityBuilder(environmentOrDefault));
        map.put(AuthType.AZURE_CLI, new AzureCliAccount());

        // map.put(AuthType.VSCODE, new VisualStudioCodeAccountEntityBuilder());
        // null is valid for visual studio account builder
        // map.put(AuthType.VISUAL_STUDIO, new VisualStudioAccountEntityBuilder(env));
        // map.put(AuthType.AZURE_AUTH_MAVEN_PLUGIN, new MavenLoginAccountEntityBuilder());
        map.put(AuthType.OAUTH2, new OAuthAccount(environmentOrDefault));
        // map.put(AuthType.DEVICE_CODE, new DeviceCodeAccountEntityBuilder(environmentOrDefault));
        return map;
    }
}
