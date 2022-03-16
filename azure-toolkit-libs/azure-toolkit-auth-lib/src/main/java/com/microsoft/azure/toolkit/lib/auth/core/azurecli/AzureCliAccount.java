/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.azurecli;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.util.IdentityConstants;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.TokenCredentialManager;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCliSubscription;
import com.microsoft.azure.toolkit.lib.auth.util.AzureCliUtils;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureCliAccount extends Account {
    @Override
    public AuthType getAuthType() {
        return AuthType.AZURE_CLI;
    }

    protected Mono<Boolean> preLoginCheck() {
        return Mono.fromCallable(() -> {
            AzureCliUtils.ensureMinimumCliVersion();
            AzureCliUtils.executeAzureCli("az account list-locations --output none"); // test whether az command is available

            final List<AzureCliSubscription> cliSubs = AzureCliUtils.listSubscriptions();
            if (cliSubs.isEmpty()) {
                throw new AzureToolkitAuthenticationException("Cannot find any subscriptions in current account.");
            }

            final AzureCliSubscription defaultSub = cliSubs.stream().filter(AzureCliSubscription::isSelected).findFirst().orElse(cliSubs.get(0));

            final AzureEnvironment configuredEnv = Azure.az(AzureCloud.class).get();
            if (configuredEnv != null && defaultSub.getEnvironment() != configuredEnv) {
                throw new AzureToolkitAuthenticationException(
                    String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                            "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                        AzureEnvironmentUtils.getCloudName(defaultSub.getEnvironment()),
                        AzureEnvironmentUtils.getCloudName(configuredEnv)));
            }

            final String userEmail = defaultSub.getEmail();
            final List<Subscription> userSubs = cliSubs.stream().filter(s -> Objects.equals(userEmail, s.getEmail())).collect(Collectors.toList());
            final List<String> userTenants = userSubs.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList());
            final List<String> userSelectedSubs = userSubs.stream().filter(Subscription::isSelected).map(Subscription::getId).collect(Collectors.toList());

            this.entity.setEnvironment(defaultSub.getEnvironment());
            this.entity.setEmail(userEmail);
            this.entity.setTenantIds(userTenants); // use the tenant who has one or more subscriptions
            this.entity.setSubscriptions(userSubs);
            this.entity.setSelectedSubscriptionIds(userSelectedSubs);
            return true;
        });
    }

    protected Mono<TokenCredentialManager> createTokenCredentialManager() {
        return Mono.just(new AzureCliTokenCredentialManager(this.entity.getEnvironment()));
    }

    @Override
    public String getClientId() {
        return IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID;
    }
}
