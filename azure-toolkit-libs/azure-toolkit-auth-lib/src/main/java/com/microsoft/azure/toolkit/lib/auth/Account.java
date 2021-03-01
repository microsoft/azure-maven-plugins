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
import com.azure.resourcemanager.resources.models.Subscription;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Account {
    private final ClientLogger logger = new ClientLogger(Account.class);
    protected AccountEntity entity;

    public Account() {
        this.entity = buildAccountEntity();
    }

    public abstract AuthMethod getMethod();

    public abstract void initializeCredentials() throws LoginFailureException;

    public AzureEnvironment getEnvironment() {
        return entity == null ? null : entity.getEnvironment();
    }

    public TokenCredential getTokenCredential(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return this.entity.getCredential();
        } else {
            return this.entity.getCredential().createRelatedTokenCredential(tenantId);
        }
    }

    public AzureTokenCredentials getTokenCredentialV1(String tenantId) {
        return AzureTokenCredentialsAdapter.from(getEnvironment(), tenantId, getTokenCredential(tenantId));
    }

    public Account logout() {
        this.entity = null;
        return this;
    }

    public boolean isAvailable() {
        return this.entity != null && this.entity.isAvailable();
    }

    public boolean isAuthenticated() {
        return this.entity != null && this.entity.isAuthenticated();
    }

    public List<SubscriptionEntity> getSubscriptions() {
        if (this.entity != null) {
            return this.entity.getSubscriptions();
        }
        return null;
    }

    public List<SubscriptionEntity> getSelectedSubscriptions() {
        if (this.entity != null) {
            return this.entity.getSubscriptions().stream().filter(SubscriptionEntity::isSelected).collect(Collectors.toList());
        }
        return null;
    }

    public void selectSubscriptions(List<String> selectedSubscriptionIds) {
        if (CollectionUtils.isNotEmpty(selectedSubscriptionIds) && CollectionUtils.isNotEmpty(this.entity.getSubscriptions())) {
            entity.getSubscriptions().forEach(s -> s.setSelected(Utils.containsIgnoreCase(selectedSubscriptionIds, s.getId())));
        }
    }

    public AccountEntity buildAccountEntity() {
        entity = createAccountEntity(getMethod());
        try {
            if (!entity.isAvailable()) {
                return entity;
            }
            entity.setAvailable(true);
            return entity;
        } catch (AzureToolkitAuthenticationException e) {
            entity.setLastError(e);
        }
        return entity;
    }

    public void authenticate() throws LoginFailureException {
        initializeCredentials();

        initializeTenants();

        initializeSubscriptions();
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

    protected void initializeTenants() throws LoginFailureException {
        MasterTokenCredential credential = entity.getCredential();
        verifyTokenCredential(credential.getEnvironment(), entity.getCredential());
        List<String> allTenantIds = listTenantIds(credential.getEnvironment(), credential);
        // in azure cli, the tenant ids from 'az account list' should be less/equal than the list tenant api
        if (this.entity.getTenantIds() == null) {
            this.entity.setTenantIds(allTenantIds);
        }
    }

    protected void initializeSubscriptions() {
        List<SubscriptionEntity> subscriptions = listSubscriptions(entity.getTenantIds(), entity.getCredential());
        entity.setTenantIds(subscriptions.stream().map(SubscriptionEntity::getTenantId).distinct().collect(Collectors.toList()));
        entity.setSubscriptions(subscriptions);
        selectSubscriptions(subscriptions, this.entity.getSelectedSubscriptionIds());
    }

    List<String> listTenantIds(AzureEnvironment environment, TokenCredential credential) {
        return AzureResourceManager.authenticate(credential
                , new AzureProfile(environment)).tenants().list().stream().map(Tenant::tenantId).collect(Collectors.toList());
    }

    private AccountEntity createAccountEntity(AuthMethod method) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setMethod(method);
        return accountEntity;
    }

    private void selectSubscriptions(List<SubscriptionEntity> subscriptions, List<String> subscriptionIds) {
        // select subscriptions
        if (CollectionUtils.isNotEmpty(subscriptionIds) && CollectionUtils.isNotEmpty(subscriptions)) {
            subscriptions.forEach(s -> s.setSelected(Utils.containsIgnoreCase(subscriptionIds, s.getId())));
        }
    }

    private List<SubscriptionEntity> listSubscriptions(List<String> tenantIds, MasterTokenCredential credential) {
        AzureProfile azureProfile = new AzureProfile(credential.getEnvironment());
        // use map to re-dup subs
        final Map<String, SubscriptionEntity> subscriptionMap = new HashMap<>();
        tenantIds.parallelStream().forEach(tenantId -> {
            try {
                TokenCredential tenantTokenCredential = credential.createRelatedTokenCredential(tenantId);
                List<SubscriptionEntity> subscriptionsOnTenant =
                        AzureResourceManager.authenticate(tenantTokenCredential, azureProfile).subscriptions().list()
                                .mapPage(s -> this.toSubscriptionEntity(tenantId, s)).stream().collect(Collectors.toList());

                for (SubscriptionEntity subscriptionEntity : subscriptionsOnTenant) {
                    String key = StringUtils.lowerCase(subscriptionEntity.getId());
                    subscriptionMap.putIfAbsent(key, subscriptionEntity);
                }
            } catch (Exception ex) {
                // ignore AuthenticationException since on some tenants, it doesn't allow list subscriptions
                if (!(ExceptionUtils.getRootCause(ex) instanceof AuthenticationException)) {
                    logger.warning("Cannot get subscriptions for tenant '%s', please verify you have all proper permission over  ");
                } else {
                    throw new AzureToolkitAuthenticationException(ex.getMessage());
                }
            }

        });

        return new ArrayList<>(subscriptionMap.values());
    }

    private static SubscriptionEntity toSubscriptionEntity(String tenantId, Subscription subscription) {
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(subscription.subscriptionId());
        subscriptionEntity.setName(subscription.displayName());
        subscriptionEntity.setTenantId(tenantId);
        return subscriptionEntity;
    }
}
