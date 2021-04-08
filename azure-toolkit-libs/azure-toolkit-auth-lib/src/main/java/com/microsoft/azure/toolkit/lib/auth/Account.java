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
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
        this.entity = new AccountEntity();
        entity.setMethod(getMethod());
    }

    public abstract AuthMethod getMethod();

    protected abstract boolean checkAvailableInner();

    protected abstract TokenCredential createTokenCredential();

    boolean checkAvailable() {
        if (this.entity.isAvailable()) {
            return true;
        }
        this.entity.setAvailable(checkAvailableInner());
        return this.entity.isAvailable();
    }

    public AzureEnvironment getEnvironment() {
        return entity == null ? null : entity.getEnvironment();
    }

    public TokenCredential getTokenCredential(String tenantId) {
        requireAuthenticated();
        if (StringUtils.isBlank(tenantId)) {
            return this.entity.getTenantCredential();
        } else {
            return this.entity.getTenantCredential().createTokenCredential(tenantId);
        }
    }

    public AzureTokenCredentials getTokenCredentialV1(String tenantId) {
        requireAuthenticated();
        return AzureTokenCredentialsAdapter.from(getEnvironment(), tenantId, getTokenCredential(tenantId));
    }

    public TokenCredential getTokenCredentialForSubscription(String subscriptionId) {
        requireAuthenticated();
        Subscription subscription = getSelectedSubscriptionById(subscriptionId);
        return getTokenCredential(subscription.getTenantId());
    }

    public AzureTokenCredentials getTokenCredentialV1ForSubscription(String subscriptionId) {
        requireAuthenticated();
        Subscription subscription = getSelectedSubscriptionById(subscriptionId);
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
        return isAvailable() && this.entity.isAuthenticated();
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

    @Override
    public String toString() {
        final List<String> details = new ArrayList<>();

        if (!this.isAuthenticated()) {
            return "<account not logged in>";
        }
        if (getMethod() != null) {
            details.add(String.format("Auth method: %s", TextUtils.cyan(getMethod().toString())));
        }
        final List<Subscription> selectedSubscriptions = getSelectedSubscriptions();
        if (StringUtils.isNotEmpty(getEntity().getEmail())) {
            details.add(String.format("Username: %s", TextUtils.cyan(getEntity().getEmail())));
        }
        if (selectedSubscriptions != null && selectedSubscriptions.size() == 1) {
            details.add(String.format("Default subscription: %s", TextUtils.cyan(selectedSubscriptions.get(0).getId())));
        }
        return StringUtils.join(details.toArray(), "\n");
    }

    void authenticate() throws LoginFailureException {
        try {
            createTokenCredential();

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
        // in azure cli, the tenant ids from 'az account list' should be less/equal than the list tenant api
        if (this.entity.getTenantIds() == null) {
            TokenCredential credential = entity.getTenantCredential().createTokenCredential(null);
            List<String> allTenantIds = listTenantIds(entity.getEnvironment(), credential);
            this.entity.setTenantIds(allTenantIds);
        }
    }

    protected void initializeSubscriptions() {
        List<Subscription> subscriptions = listSubscriptions(entity.getTenantIds(), entity.getTenantCredential());
        entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));
        entity.setSubscriptions(subscriptions);
        selectSubscriptionInner(subscriptions, this.entity.getSelectedSubscriptionIds());

        // no subscriptions are selected, selected all
        if (!subscriptions.isEmpty() && subscriptions.stream().noneMatch(Subscription::isSelected)) {
            subscriptions.forEach(s -> s.setSelected(true));
        }
    }

    private Subscription getSelectedSubscriptionById(String subscriptionId) {
        return getSelectedSubscriptions().stream()
                .filter(s -> StringUtils.equalsIgnoreCase(subscriptionId, s.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find subscription with id '%s'", subscriptionId)));
    }

    private void selectSubscriptionInner(List<Subscription> subscriptions, List<String> subscriptionIds) {
        // select subscriptions
        if (CollectionUtils.isNotEmpty(subscriptionIds) && CollectionUtils.isNotEmpty(subscriptions)) {
            subscriptions.forEach(s -> s.setSelected(Utils.containsIgnoreCase(subscriptionIds, s.getId())));
        }
    }

    private List<Subscription> listSubscriptions(List<String> tenantIds, TenantCredential credential) {
        AzureProfile azureProfile = new AzureProfile(entity.getEnvironment());
        // use map to re-dup subs
        final Map<String, Subscription> subscriptionMap = new HashMap<>();
        tenantIds.parallelStream().forEach(tenantId -> {
            try {
                TokenCredential tenantTokenCredential = credential.createTokenCredential(tenantId);
                List<Subscription> subscriptionsOnTenant =
                        AzureResourceManager.authenticate(tenantTokenCredential, azureProfile).subscriptions().list()
                                .stream().map(s -> toSubscriptionEntity(tenantId, s)).collect(Collectors.toList());

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

    private void requireAuthenticated() {
        if (!this.isAuthenticated()) {
            throw new AzureToolkitAuthenticationException("Please signed in first.");
        }
    }
}
