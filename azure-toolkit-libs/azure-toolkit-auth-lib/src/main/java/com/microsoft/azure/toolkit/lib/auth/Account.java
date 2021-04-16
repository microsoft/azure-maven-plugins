/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public abstract class Account implements IAccount {
    @Getter
    protected AccountEntity entity;

    protected TokenCredentialManager credentialManager;

    public Account() {
        this.entity = new AccountEntity();
    }

    public abstract AuthType getAuthType();

    protected abstract String getClientId();

    public AzureEnvironment getEnvironment() {
        return entity == null ? null : entity.getEnvironment();
    }

    protected abstract Mono<Boolean> preLoginCheck();

    public boolean isAvailable() {
        return this.entity.isAvailable();
    }

    public TokenCredential getTokenCredentialForTenant(String tenantId) {
        requireAuthenticated();
        if (StringUtils.isBlank(tenantId)) {
            throw new IllegalArgumentException("Should provide non-empty tenant id for retrieving credential.");
        } else {
            return this.credentialManager.createTokenCredentialForTenant(tenantId);
        }
    }

    public AzureTokenCredentials getTokenCredentialForTenantV1(String tenantId) {
        requireAuthenticated();
        return AzureTokenCredentialsAdapter.from(getEnvironment(), tenantId, getTokenCredentialForTenant(tenantId));
    }

    public TokenCredential getTokenCredential(String subscriptionId) {
        requireAuthenticated();
        Subscription subscription = getSelectedSubscriptionById(subscriptionId);
        return getTokenCredentialForTenant(subscription.getTenantId());
    }

    public AzureTokenCredentials getTokenCredentialV1(String subscriptionId) {
        requireAuthenticated();
        Subscription subscription = getSelectedSubscriptionById(subscriptionId);
        return getTokenCredentialForTenantV1(subscription.getTenantId());
    }

    public void logout() {
        if (this.entity != null) {
            this.entity = null;
            Azure.az(AzureAccount.class).logout();
        }
    }

    public List<Subscription> getSubscriptions() {
        requireAuthenticated();
        return this.entity.getSubscriptions();
    }

    @Override
    public List<Subscription> getSelectedSubscriptions() {
        requireAuthenticated();
        return this.entity.getSubscriptions().stream().filter(Subscription::isSelected).collect(Collectors.toList());
    }

    public void selectSubscription(List<String> selectedSubscriptionIds) {
        requireAuthenticated();
        if (CollectionUtils.isEmpty(selectedSubscriptionIds)) {
            throw new IllegalArgumentException("You must select at least one subscription.");
        }

        if (CollectionUtils.isEmpty(getSubscriptions())) {
            throw new IllegalArgumentException("There are no subscriptions to select.");
        }
        if (entity.getSubscriptions().stream().anyMatch(s -> Utils.containsIgnoreCase(selectedSubscriptionIds, s.getId()))) {
            selectSubscriptionInner(this.getSubscriptions(), selectedSubscriptionIds);
        } else {
            throw new AzureToolkitAuthenticationException("Cannot select subscriptions since none subscriptions are selected, " +
                    "make sure you have provided valid subscription list");
        }
    }

    protected Mono<TokenCredentialManager> initializeTokenCredentialManager() {
        return this.preLoginCheck()// step 1: check avail //TODO: rename
                .flatMap(ignore -> {
                    // step 2: create TokenCredentialManager
                    return createTokenCredentialManager();
                });
    }

    protected abstract Mono<TokenCredentialManager> createTokenCredentialManager();

    public Mono<Boolean> checkAvailable() {
        return loginStep1();
    }

    protected Mono<Account> login() {
        Mono<Boolean> mono = loginStep1();
        return mono.flatMap(ignore -> {
            if (this.entity.getSubscriptions() == null) {
                return this.credentialManager.listSubscriptions(this.entity.getTenantIds())
                        .map(subscriptions -> {
                            // reset tenant id again when all subscriptions
                            entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));
                            entity.setSubscriptions(subscriptions);
                            return true;
                        });
            }
            return Mono.just(true);
        }).map(ignore -> {
            finishLogin();
            return this;
        });
    }

    private void finishLogin() {
        this.entity.setAuthenticated(true);
        selectSubscriptionInner(getSubscriptions(), this.entity.getSelectedSubscriptionIds());
        // select all when no subs are selected
        if (this.getSelectedSubscriptions().isEmpty()) {
            getSubscriptions().forEach(subscription -> subscription.setSelected(true));
        }
    }

    /***
     * Login step 1: check avail and set avail in entity
     *
     * @return Mono = true if this account is available
     */
    private Mono<Boolean> loginStep1() {
        return initializeTokenCredentialManager().flatMap(credentialManager -> {
            this.credentialManager = credentialManager;
            return this.credentialManager.listTenants();
        }).doOnSuccess(tenantIds -> {
            if (this.entity.getTenantIds() == null) {
                this.entity.setTenantIds(tenantIds);
            }
            if (StringUtils.isNotBlank(credentialManager.getEmail())) {
                entity.setEmail(credentialManager.getEmail());
            }
            entity.setEnvironment(credentialManager.getEnvironment());
        }).map(ignore -> {
            this.entity.setAvailable(true);
            return true;
        });
    }

    @Override
    public String toString() {
        final List<String> details = new ArrayList<>();

        if (!this.entity.isAvailable() || !this.entity.isAuthenticated()) {
            return "<account not logged in>";
        }
        if (getAuthType() != null) {
            details.add(String.format("Auth type: %s", TextUtils.cyan(getAuthType().toString())));
        }
        if (this.entity.isAvailable() && CollectionUtils.isNotEmpty(getSubscriptions())) {
            final List<Subscription> selectedSubscriptions = getSelectedSubscriptions();
            if (selectedSubscriptions != null && selectedSubscriptions.size() == 1) {
                details.add(String.format("Default subscription: %s(%s)", TextUtils.cyan(selectedSubscriptions.get(0).getName()),
                        TextUtils.cyan(selectedSubscriptions.get(0).getId())));
            }
        }

        if (StringUtils.isNotEmpty(getEntity().getEmail())) {
            details.add(String.format("Username: %s", TextUtils.cyan(getEntity().getEmail())));
        }

        return StringUtils.join(details.toArray(), "\n");
    }

    private Subscription getSelectedSubscriptionById(String subscriptionId) {
        return getSelectedSubscriptions().stream()
                .filter(s -> StringUtils.equalsIgnoreCase(subscriptionId, s.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find subscription with id '%s'", subscriptionId)));
    }

    private static void selectSubscriptionInner(List<Subscription> subscriptions, List<String> subscriptionIds) {
        // select subscriptions
        if (CollectionUtils.isNotEmpty(subscriptionIds) && CollectionUtils.isNotEmpty(subscriptions)) {
            subscriptions.forEach(s -> s.setSelected(Utils.containsIgnoreCase(subscriptionIds, s.getId())));
        }
    }

    private void requireAuthenticated() {
        if (!this.entity.isAvailable() || !this.entity.isAuthenticated()) {
            throw new AzureToolkitAuthenticationException("Please signed in first.");
        }
    }
}
