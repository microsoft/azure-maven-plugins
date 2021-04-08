/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.core.oauth.OAuthAccount;
import com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal.ServicePrincipalAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AzureAccount implements AzureService, IAzureAccount {

    @Setter(AccessLevel.PRIVATE)
    private Account account;

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
                .orElseThrow(() -> new AzureToolkitAuthenticationException("Please signed in first."));
    }

    public List<Account> accounts() {
        List<Account> accountList = Flux.fromIterable(buildAccountMap().values()).map(Supplier::get).collectList().block();
        return accountList.stream().filter(Account::checkAvailable).collect(Collectors.toList());
    }

    public Account account(AuthType type) {
        Account ac = buildAccountMap().get(type).get();
        ac.checkAvailable();
        return ac;
    }

    public AzureAccount login(@Nonnull AuthType type) {
        return blockMonoAndReturnThis(loginAsync(type));
    }

    public AzureAccount login(@Nonnull Account targetAccount) {
        return blockMonoAndReturnThis(loginAsync(targetAccount));
    }

    public AzureAccount login(@Nonnull AuthConfiguration auth) {
        return blockMonoAndReturnThis(loginAsync(auth));
    }

    public void logout() {
        if (this.account != null) {
            this.account.logout();
            this.account = null;
        }
    }

    public boolean isAuthenticated() {
        return this.account != null && this.account.isAvailable() && this.account.isAuthenticated();
    }

    public Mono<Account> authenticateAccounts(List<Account> accounts) {
        Mono<Account> current = accounts.get(0).authenticate();
        for (int i = 1; i < accounts.size(); i++) {
            final Account ac = accounts.get(i);
            current = current.onErrorResume(e -> ac.authenticate());
        }
        return current;
    }

    public Mono<Account> loginAsync(@Nonnull AuthConfiguration auth) {
        // update the env state of AzureAccount when auth configuration has an explicit configuration of env
        Objects.requireNonNull(auth, "Auth configuration is required for login.");
        Objects.requireNonNull(auth.getType(), "Auth type is required for login.");
        AuthType type = auth.getType();
        final List<Account> accounts = new ArrayList<>();
        if (auth.getType() == AuthType.SERVICE_PRINCIPAL) {
            accounts.add(new ServicePrincipalAccount(auth));
        } else {
            Map<AuthType, Supplier<Account>> accountByType = buildAccountMap();
            if (auth.getType() == AuthType.AUTO) {
                accounts.add(new ServicePrincipalAccount(auth));
                accounts.addAll(accountByType.values().stream().map(Supplier::get).collect(Collectors.toList()));
            } else {
                if (!accountByType.containsKey(type)) {
                    return Mono.error(new LoginFailureException(String.format("Unsupported auth type '%s', supported values are: %s.\"",
                            type, accountByType.keySet().stream().map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", ")))));
                }
                accounts.add(accountByType.get(type).get());
            }
        }

        return authenticateAccounts(accounts).doOnSuccess(result -> {
            checkEnv(result, auth.getEnvironment());
            this.setAccount(result);
        });
    }

    public Mono<Account> loginAsync(AuthType type) {
        Objects.requireNonNull(type, "Please specify auth type in auth configuration.");
        Map<AuthType, Supplier<Account>> accountByType = buildAccountMap();
        if (!accountByType.containsKey(type)) {
            return Mono.error(new LoginFailureException(String.format("Unsupported auth type '%s', supported values are: %s.\"",
                    type, accountByType.keySet().stream().map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", ")))));
        }
        return loginAsync(accountByType.get(type).get());
    }

    public Mono<Account> loginAsync(Account targetAccount) {
        Objects.requireNonNull(targetAccount, "Please specify account to login.");

        return targetAccount.authenticate().doOnSuccess(this::setAccount);
    }

    private AzureAccount blockMonoAndReturnThis(Mono<Account> mono) {
        try {
            mono.block();
            return this;
        } catch (Throwable ex) {
            throw new AzureToolkitAuthenticationException("Cannot login due to error: " + ex.getMessage());
        }
    }

    private static void checkEnv(Account ac, AzureEnvironment env) {

        if (env != null && ac.getEnvironment() != null
                && ac.getEnvironment() != env
                && ac.isAuthenticated()) {

            String expectedEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(env);
            String realEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(ac.getEnvironment());

            // conflicting configuration of azure environment
            switch (ac.getMethod()) {
                case AZURE_CLI:
                    throw new AzureToolkitAuthenticationException(
                            String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                                    "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                            realEnv,
                            expectedEnv));

                case AZURE_SECRET_FILE:
                    throw new AzureToolkitAuthenticationException(
                            String.format("The azure cloud from maven login '%s' doesn't match with your auth configuration, " +
                                    "please switch to other auth method for '%s' environment.",
                            realEnv,
                            expectedEnv));
                case VSCODE:
                    throw new AzureToolkitAuthenticationException(
                            String.format("The azure cloud from vscode '%s' doesn't match with your auth configuration: %s, " +
                                    "you can change it by pressing F1 in VSCode and find \">azure: sign in to Azure Cloud\" command " +
                                    "to change azure cloud in vscode.",
                            realEnv,
                            expectedEnv));
                default: // empty

            }
        }
    }

    private static Map<AuthType, Supplier<Account>> buildAccountMap() {
        Map<AuthType, Supplier<Account>> map = new LinkedHashMap<>();
        // SP is not there since it requires special constructor argument and it is special(it requires complex auth configuration)
        map.put(AuthType.AZURE_CLI, AzureCliAccount::new);
        map.put(AuthType.OAUTH2, OAuthAccount::new);

        return map;
    }
}
