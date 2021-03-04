/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.logging.ClientLogger;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.core.oauth.OAuthAccount;
import com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal.ServicePrincipalAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAccount implements AzureService, IAzureAccount {

    private final ClientLogger logger = new ClientLogger(AzureAccount.class);

    private Account account;

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
                .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not initialized."));
    }

    public List<Account> accounts() {
        return buildAccountMap(null).values().stream().filter(Account::checkAvailable).collect(Collectors.toList());
    }

    public AzureAccount login(@Nonnull Account targetAccount) throws LoginFailureException {
        account = targetAccount;
        if (account.checkAvailable()) {
            account.authenticate();
        } else {
            if (account.entity.getLastError() != null) {
                throw new LoginFailureException(account.entity.getLastError().getMessage(), account.entity.getLastError());
            } else {
                throw new LoginFailureException(String.format("Cannot get credential from auth method '%s'.", targetAccount.getMethod()));
            }
        }
        return this;
    }

    public AzureAccount login(AuthType type) throws LoginFailureException {
        AuthConfiguration auth = new AuthConfiguration();
        auth.setType(type);
        loginWithAuthConfiguration(auth);
        return this;
    }

    public void login(@Nonnull AuthConfiguration auth) throws LoginFailureException {
        // update the env state of AzureAccount when auth configuration has a strong configuration of env
        Objects.requireNonNull(auth, "Null auth configuration is illegal for login.");
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

    private void loginWithAuthConfiguration(@Nonnull AuthConfiguration auth) throws LoginFailureException {
        Objects.requireNonNull(auth, "Null 'auth' cannot be used to sign-in.");
        Objects.requireNonNull(auth.getType(), "Please specify auth type in auth configuration.");
        Map<AuthType, Account> accountByType = buildAccountMap(auth.getEnvironment());
        if (auth.getType() == AuthType.SERVICE_PRINCIPAL || auth.getType() == AuthType.AUTO) {
            if (loginServicePrincipal(auth)) {
                return;
            }
        }
        if (accountByType.containsKey(auth.getType())) {
            login(accountByType.get(auth.getType()));
        } else {
            throw new LoginFailureException(String.format("Unsupported auth type '%s', supported values are: %s.\"",
                    auth.getType(), accountByType.keySet().stream().map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", "))));
        }
    }

    private boolean loginServicePrincipal(AuthConfiguration auth) throws LoginFailureException {
        boolean forceServicePrincipalLogin = auth.getType() == AuthType.SERVICE_PRINCIPAL;
        boolean isSPConfigurationPresent = !StringUtils.isAllBlank(auth.getCertificate(), auth.getKey(),
                auth.getCertificatePassword());
        try {
            ServicePrincipalAccount spAccount = new ServicePrincipalAccount(auth);
            login(spAccount);
            return spAccount.isAuthenticated();
        } catch (LoginFailureException | AzureToolkitAuthenticationException e) {
            if (forceServicePrincipalLogin) {
                throw new LoginFailureException(e.getMessage(), e);
            }
            if (isSPConfigurationPresent) {
                logger.warning("Cannot login through 'SERVICE_PRINCIPAL' due to invalid configuration: " + e.getMessage());
            }
        }
        return false;
    }

    private static Map<AuthType, Account> buildAccountMap(AzureEnvironment env) {
        Map<AuthType, Account> map = new LinkedHashMap<>();
        // SP is not there since it requires special constructor argument and it is handled in login(AuthConfiguration auth)
        AzureEnvironment environmentOrDefault = ObjectUtils.firstNonNull(env, AzureEnvironment.AZURE);
        // map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityAccount(environmentOrDefault));
        map.put(AuthType.AZURE_CLI, new AzureCliAccount());

        // map.put(AuthType.VSCODE, new VisualStudioCodeAccount());
        // null is valid for visual studio account
        // map.put(AuthType.VISUAL_STUDIO, new VisualStudioAccount(env));
        // map.put(AuthType.AZURE_AUTH_MAVEN_PLUGIN, new MavenLoginAccount());
        map.put(AuthType.OAUTH2, new OAuthAccount(environmentOrDefault));
        // map.put(AuthType.DEVICE_CODE, new DeviceCodeAccount(environmentOrDefault));
        return map;
    }
}
