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
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.Preloader;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public abstract class Account implements IAccount {
    protected static final String TOOLKIT_TOKEN_CACHE_NAME = "azure-toolkit.cache";

    @Getter
    protected AccountEntity entity;

    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PROTECTED)
    protected boolean enablePersistence = false;

    protected TokenCredentialManager credentialManager;

    public Account() {
        this.entity = new AccountEntity();
    }

    public abstract AuthType getAuthType();

    protected abstract String getClientId();

    public AzureEnvironment getEnvironment() {
        return entity == null ? null : entity.getEnvironment();
    }

    public String portalUrl() {
        return AzureEnvironmentUtils.getPortalUrl(this.getEnvironment());
    }

    protected abstract Mono<Boolean> preLoginCheck();

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

    @CacheEvict(CacheEvict.ALL) // evict all caches on signing out
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
            final AzureTaskManager manager = AzureTaskManager.getInstance();
            if (Objects.nonNull(manager)) {
                manager.runOnPooledThread(Preloader::load);
            }
        } else {
            throw new AzureToolkitAuthenticationException("no subscriptions are selected, " +
                    "make sure you have provided valid subscription list");
        }
    }

    private Mono<TokenCredentialManager> initializeTokenCredentialManager() {
        return createTokenCredentialManager().doOnSuccess(tokenCredentialManager -> this.credentialManager = tokenCredentialManager);
    }

    protected abstract Mono<TokenCredentialManager> createTokenCredentialManager();

    public Mono<Boolean> checkAvailable() {
        return preLoginCheck().doOnSuccess(avail -> this.entity.setAvailable(avail));
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

    public Mono<Account> continueLogin() {
        Azure.az(AzureAccount.class).setAccount(this);
        return Mono.just(this);
    }

    private void finishLogin() {
        selectSubscriptionInner(getSubscriptions(), this.entity.getSelectedSubscriptionIds());
        // select all when no subs are selected
        if (this.getSelectedSubscriptions().isEmpty()) {
            getSubscriptions().forEach(subscription -> subscription.setSelected(true));
            this.entity.setSelectedSubscriptionIds(getSubscriptions().stream().map(Subscription::getId).collect(Collectors.toList()));
        }
    }

    public Mono<List<Subscription>> reloadSubscriptions() {
        List<String> beforeRefreshSelectedSubsIds = this.getSelectedSubscriptions().stream().map(Subscription::getId).collect(Collectors.toList());
        return credentialManager.listTenants().flatMap(tenantIds -> this.credentialManager.listSubscriptions(tenantIds)
                .map(subscriptions -> {
                    // reset tenant id again when all subscriptions
                    entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));
                    entity.setSubscriptions(subscriptions);
                    this.selectSubscription(beforeRefreshSelectedSubsIds);
                    return this.getSubscriptions();
                }));
    }

    /***
     * The main part of login process: check available and initialize TokenCredentialManager and list tenant ids
     *
     * @return Mono = true if this account is available
     */
    private Mono<Boolean> loginStep1() {
        // step 1: check avail
        // step 2: create TokenCredentialManager
        // step 3: list tenant using TokenCredentialManager
        // step 4: fill account entity
        return checkAvailable().flatMap(ignore -> initializeTokenCredentialManager()).flatMap(this::loadTenantIdsIfAbsent).doOnSuccess(tenantIds -> {
            this.entity.setType(this.getAuthType());
            this.entity.setClientId(this.getClientId());
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

    private Mono<List<String>> loadTenantIdsIfAbsent(TokenCredentialManager tokenCredentialManager) {
        if (CollectionUtils.isNotEmpty(this.entity.getTenantIds())) {
            return Mono.just(this.entity.getTenantIds());
        }
        return tokenCredentialManager.listTenants();
    }

    @Override
    public String toString() {
        final List<String> details = new ArrayList<>();

        if (!this.entity.isAvailable()) {
            return "<account not available>";
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

    @Override
    public Subscription getSubscription(String subscriptionId) {
        return getSubscriptions().stream()
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
        if (!this.entity.isAvailable()) {
            throw new AzureToolkitAuthenticationException("account is not available.");
        }
        if (this.credentialManager == null || this.entity.getTenantIds() == null || this.entity.getSubscriptions() == null) {
            throw new AzureToolkitAuthenticationException("you are not signed-in.");
        }
    }
}
