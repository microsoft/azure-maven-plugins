/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.identity.implementation.util.ScopeUtil;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Account implements IAccount {
    private final ClientLogger logger = new ClientLogger(Account.class);
    @Getter
    protected AccountEntity entity;

    public Account() {
        buildAccountEntity();
    }

    public abstract AuthMethod getMethod();

    protected abstract Mono<Boolean> checkAvailableInner();

    protected abstract void initializeCredentials() throws LoginFailureException;

    boolean checkAvailable() {
        checkAvailableInner().doOnSuccess(avail -> this.entity.setAvailable(avail)).onErrorContinue((e, i) -> this.entity.setLastError(e)).block();
        return isAvailable();
    }

    public AzureEnvironment getEnvironment() {
        return entity == null ? null : entity.getEnvironment();
    }

    public TokenCredential getTokenCredential(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return this.entity.getCredential();
        } else {
            return this.entity.getCredential().createTenantTokenCredential(tenantId);
        }
    }

    public AzureTokenCredentials getTokenCredentialV1(String tenantId) {
        return AzureTokenCredentialsAdapter.from(getEnvironment(), tenantId, getTokenCredential(tenantId));
    }

    public TokenCredential getTokenCredentialForSubscription(String subscriptionId) {
        Subscription subscription = getSubscriptionById(subscriptionId);
        return getTokenCredential(subscription.getTenantId());
    }

    public AzureTokenCredentials getTokenCredentialV1ForSubscription(String subscriptionId) {
        Subscription subscription = getSubscriptionById(subscriptionId);
        return getTokenCredentialV1(subscription.getTenantId());
    }

    public Account logout() {
        this.entity = null;
        return this;
    }

    public boolean isAvailable() {
        return this.entity != null && this.entity.isAvailable();
    }

    public boolean isAuthenticated() {
        return isAvailable() && this.entity != null && this.entity.isAuthenticated();
    }

    public List<Subscription> getSubscriptions() {
        if (this.entity != null) {
            return this.entity.getSubscriptions();
        }
        return null;
    }

    @Override
    public List<Subscription> getSelectedSubscriptions() {
        if (this.entity != null) {
            return this.entity.getSubscriptions().stream().filter(Subscription::isSelected).collect(Collectors.toList());
        }
        return null;
    }

    public void selectSubscriptions(List<String> selectedSubscriptionIds) {
        if (CollectionUtils.isNotEmpty(selectedSubscriptionIds) && CollectionUtils.isNotEmpty(this.entity.getSubscriptions())) {
            entity.getSubscriptions().forEach(s -> s.setSelected(Utils.containsIgnoreCase(selectedSubscriptionIds, s.getId())));
        }
    }

    void authenticate() throws LoginFailureException {
        try {
            initializeCredentials();

            initializeTenants();

            initializeSubscriptions();

            entity.setAuthenticated(true);
        } catch (AzureToolkitAuthenticationException e) {
            entity.setLastError(e);
        }
    }

    List<String> listTenantIds(AzureEnvironment environment, TokenCredential credential) {
        return AzureResourceManager.authenticate(credential
                , new AzureProfile(environment)).tenants().list().stream().map(Tenant::tenantId).collect(Collectors.toList());
    }

    protected void buildAccountEntity() {
        try {
            createAccountEntity(getMethod());
        } catch (AzureToolkitAuthenticationException e) {
            entity.setLastError(e);
        }
    }

    protected void verifyTokenCredential(AzureEnvironment environment, TokenCredential credential) throws LoginFailureException {
        try {
            TokenRequestContext tokenRequestContext = new TokenRequestContext()
                    .addScopes(ScopeUtil.resourceToScopes(environment.getManagementEndpoint()));
            credential.getToken(tokenRequestContext).block().getToken();
        } catch (Throwable ex) {
            throw new LoginFailureException(ex.getMessage());
        }
    }

    protected void initializeTenants() {
        TokenCredential credential = entity.getCredential().createTenantTokenCredential(null);
        List<String> allTenantIds = listTenantIds(entity.getCredential().getEnvironment(), credential);
        // in azure cli, the tenant ids from 'az account list' should be less/equal than the list tenant api
        if (this.entity.getTenantIds() == null) {
            this.entity.setTenantIds(allTenantIds);
        }
    }

    protected void initializeSubscriptions() {
        List<Subscription> subscriptions = listSubscriptions(entity.getTenantIds(), entity.getCredential());
        entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));
        entity.setSubscriptions(subscriptions);
        selectSubscriptions(subscriptions, this.entity.getSelectedSubscriptionIds());
    }

    private Subscription getSubscriptionById(String subscriptionId) {
        return getSubscriptions().stream()
                .filter(s -> StringUtils.equalsIgnoreCase(subscriptionId, s.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find subscription with id '%s'", subscriptionId)));
    }

    private void createAccountEntity(AuthMethod method) {
        this.entity = new AccountEntity();
        entity.setMethod(method);
    }

    private void selectSubscriptions(List<Subscription> subscriptions, List<String> subscriptionIds) {
        // select subscriptions
        if (CollectionUtils.isNotEmpty(subscriptionIds) && CollectionUtils.isNotEmpty(subscriptions)) {
            subscriptions.forEach(s -> s.setSelected(Utils.containsIgnoreCase(subscriptionIds, s.getId())));
        }
    }

    private List<Subscription> listSubscriptions(List<String> tenantIds, BaseTokenCredential credential) {
        AzureProfile azureProfile = new AzureProfile(credential.getEnvironment());
        // use map to re-dup subs
        final Map<String, Subscription> subscriptionMap = new HashMap<>();
        tenantIds.parallelStream().forEach(tenantId -> {
            try {
                TokenCredential tenantTokenCredential = credential.createTenantTokenCredential(tenantId);
                List<Subscription> subscriptionsOnTenant =
                        AzureResourceManager.authenticate(tenantTokenCredential, azureProfile).subscriptions().list()
                                .mapPage(s -> toSubscriptionEntity(tenantId, s)).stream().collect(Collectors.toList());

                for (Subscription subscription : subscriptionsOnTenant) {
                    String key = StringUtils.lowerCase(subscription.getId());
                    subscriptionMap.putIfAbsent(key, subscription);
                }
            } catch (Exception ex) {
                // ignore AuthenticationException since on some tenants, it doesn't allow list subscriptions
                if ((ExceptionUtils.getRootCause(ex) instanceof AuthenticationException)) {
                    logger.warning("Cannot get subscriptions for tenant " + tenantId +
                            ", please verify you have proper permissions over this tenant.");
                } else {
                    throw new AzureToolkitAuthenticationException(ex.getMessage());
                }
            }

        });

        return new ArrayList<>(subscriptionMap.values());
    }

    private static Subscription toSubscriptionEntity(String tenantId, com.azure.resourcemanager.resources.models.Subscription subscription) {
        final Subscription subscriptionEntity = new Subscription();
        subscriptionEntity.setId(subscription.subscriptionId());
        subscriptionEntity.setName(subscription.displayName());
        subscriptionEntity.setTenantId(tenantId);
        return subscriptionEntity;
    }
}
