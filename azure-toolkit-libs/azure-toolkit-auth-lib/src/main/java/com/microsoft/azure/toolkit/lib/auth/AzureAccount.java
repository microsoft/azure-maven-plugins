/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
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
        return Flux.fromIterable(buildAccountMap().values()).map(Supplier::get).collectList().block();

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
            Account tempAccount = this.account;
            this.account = null;
            tempAccount.logout();
        }
    }

    public Mono<Account> loginAsync(AuthType type) {
        Objects.requireNonNull(type, "Please specify auth type in auth configuration.");
        AuthConfiguration auth = new AuthConfiguration();
        auth.setType(type);
        return loginAsync(auth);
    }

    public Mono<Account> loginAsync(@Nonnull AuthConfiguration auth) {
        Objects.requireNonNull(auth, "Auth configuration is required for login.");
        Objects.requireNonNull(auth.getType(), "Auth type is required for login.");
        Preconditions.checkArgument(auth.getType() != AuthType.AUTO, "Auth type 'auto' is illegal for login.");

        AuthType type = auth.getType();
        final Account targetAccount;
        if (auth.getType() == AuthType.SERVICE_PRINCIPAL) {
            targetAccount = new ServicePrincipalAccount(auth);
        } else {
            Map<AuthType, Supplier<Account>> accountByType = buildAccountMap();
            if (!accountByType.containsKey(type)) {
                return Mono.error(new LoginFailureException(String.format("Unsupported auth type '%s', supported values are: %s.\"",
                        type, accountByType.keySet().stream().map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", ")))));
            }
            targetAccount = accountByType.get(type).get();
        }

        return loginAsync(targetAccount).doOnSuccess(ignore -> checkEnv(targetAccount, auth.getEnvironment()));
    }

    public Mono<Account> loginAsync(Account targetAccount) {
        Objects.requireNonNull(targetAccount, "Please specify account to login.");
        return targetAccount.login().doOnSuccess(this::setAccount);
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
        if (env != null && ac.getEnvironment() != null && ac.getEnvironment() != env && ac.isAvailable()) {
            String expectedEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(env);
            String realEnv = AzureEnvironmentUtils.getCloudNameForAzureCli(ac.getEnvironment());

            // conflicting configuration of azure environment
            switch (ac.getAuthType()) {
                case AZURE_CLI:
                    throw new AzureToolkitAuthenticationException(
                            String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                                            "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                                    realEnv,
                                    expectedEnv));

                case AZURE_AUTH_MAVEN_PLUGIN:
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
        map.put(AuthType.DEVICE_CODE, DeviceCodeAccount::new);
        return map;
    }
}
