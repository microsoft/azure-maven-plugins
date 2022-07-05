/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.SimpleTokenCache;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.http.policy.FixedDelay;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.identity.TokenCachePersistenceOptions;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.azure.identity.implementation.util.ScopeUtil;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.Preloader;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public abstract class Account implements IAccount {
    protected static final TokenCachePersistenceOptions PERSISTENCE_OPTIONS = new TokenCachePersistenceOptions().setName("azure-toolkit.cache");
    private static final ClientLogger LOGGER = new ClientLogger(Account.class);
    private final Map<String, TokenCredential> tenantCredentialCache = new ConcurrentHashMap<>();
    private final AuthType authType;
    private final String clientId;
    protected String username;
    @Setter(AccessLevel.PACKAGE)
    protected boolean persistenceEnabled = true;
    @Getter(AccessLevel.PACKAGE)
    private TokenCredential defaultTokenCredential;
    @Getter(AccessLevel.NONE)
    private List<Subscription> subscriptions;

    public Account(@Nonnull AuthType authType) {
        this.authType = authType;
        this.clientId = IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID;
    }

    public Account(@Nonnull AuthType authType, @Nonnull String clientId) {
        this.authType = authType;
        this.clientId = clientId;
    }

    @Nonnull
    protected abstract TokenCredential buildDefaultTokenCredential();

    public TokenCredential getTokenCredential(String subscriptionId) {
        final Subscription subscription = getSubscription(subscriptionId);
        return getTenantTokenCredential(subscription.getTenantId());
    }

    @Nonnull
    public TokenCredential getTenantTokenCredential(@Nonnull String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new IllegalArgumentException("tenant id is required to retrieve credential.");
        } else {
            return this.tenantCredentialCache.computeIfAbsent(tenantId, tId -> new TenantTokenCredential(tId, this.defaultTokenCredential));
        }
    }

    void login() {
        this.defaultTokenCredential = this.buildDefaultTokenCredential();
        this.subscriptions = Optional.ofNullable(this.loadSubscriptions()).orElse(Collections.emptyList()).stream()
            .sorted(Comparator.comparing(s -> s.getName().toLowerCase()))
            .collect(Collectors.toList());
        this.setupAfterLogin(this.defaultTokenCredential);
    }

    public abstract boolean checkAvailable();

    @Nonnull
    protected Optional<AccessToken> getManagementToken() {
        final String[] scopes = ScopeUtil.resourceToScopes(this.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
        try {
            return this.buildDefaultTokenCredential().getToken(request).blockOptional();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    protected void setupAfterLogin(TokenCredential defaultTokenCredential) {
        final String[] scopes = ScopeUtil.resourceToScopes(this.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
        final AccessToken token = defaultTokenCredential.getToken(request).blockOptional()
            .orElseThrow(() -> new AzureToolkitAuthenticationException("Failed to retrieve token."));
        if (token instanceof MsalToken) {
            this.username = ((MsalToken) token).getAccount().username();
        }
    }

    @CacheEvict(CacheEvict.ALL)
        // evict all caches on signing out
    void logout() {
        this.subscriptions = null;
        this.defaultTokenCredential = null;
    }

    protected List<Subscription> loadSubscriptions() {
        return loadTenants().flatMap(this::loadSubscriptions).distinct(s -> s.getId().toLowerCase()).collectList().block();
    }

    @Nonnull
    private Flux<String> loadTenants() {
        final TokenCredential credential = this.defaultTokenCredential;
        final ResourceManager.Authenticated client = configureAzure().authenticate(credential, new AzureProfile(this.getEnvironment()));
        return client.tenants().listAsync().map(Tenant::tenantId).collectList().flatMapIterable(s -> s);
    }

    @Nonnull
    private Flux<Subscription> loadSubscriptions(String tenantId) {
        final TokenCredential credential = this.getTenantTokenCredential(tenantId);
        final ResourceManager.Authenticated client = configureAzure().authenticate(credential, new AzureProfile(tenantId, null, this.getEnvironment()));
        return client.subscriptions().listAsync().onErrorResume(ex -> { // warn and ignore, should modify here if IMessage is ready
                AzureMessager.getMessager().warning(String.format("Cannot get subscriptions for tenant %s " +
                    ", please verify you have proper permissions over this tenant, detailed error: %s", tenantId, ex.getMessage()));
                return Flux.fromIterable(new ArrayList<>());
            }).map(Subscription::new)
            .collect(Collectors.toList()).flatMapIterable(s -> s);
    }

    @Nonnull
    public List<Subscription> getSubscriptions() {
        if (!this.isLoggedIn()) {
            final String cause = "You are not signed-in or there are no subscriptions in your current Account.";
            throw new AzureToolkitRuntimeException(cause, IAccountActions.AUTHENTICATE, IAccountActions.TRY_AZURE);
        }
        return new ArrayList<>(Optional.ofNullable(this.subscriptions).orElse(Collections.emptyList()));
    }

    public void setSelectedSubscriptions(List<String> selectedSubscriptionIds) {
        if (CollectionUtils.isEmpty(selectedSubscriptionIds)) {
            throw new AzureToolkitRuntimeException("No subscriptions are selected. You must select at least one subscription.", IAccountActions.SELECT_SUBS);
        }
        if (this.getSubscriptions().stream().anyMatch(s -> Utils.containsIgnoreCase(selectedSubscriptionIds, s.getId()))) {
            markSubscriptionsSelected(selectedSubscriptionIds);
            AzureEventBus.emit("account.subscription_changed.account", this);
            final AzureTaskManager manager = AzureTaskManager.getInstance();
            if (Objects.nonNull(manager)) {
                manager.runOnPooledThread(Preloader::load);
            }
        } else {
            throw new AzureToolkitRuntimeException("the selected subscriptions are invalid", IAccountActions.SELECT_SUBS);
        }
    }

    private void markSubscriptionsSelected(@Nonnull List<String> subscriptionIds) {
        final Set<String> set = subscriptionIds.stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.getSubscriptions().stream()
            .filter(s -> set.contains(s.getId().toLowerCase()))
            .forEach(s -> s.setSelected(true));
    }

    @Override
    public Subscription getSubscription(String subscriptionId) {
        return this.getSubscriptions().stream()
            .filter(s -> StringUtils.equalsIgnoreCase(subscriptionId, s.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find subscription with id '%s'", subscriptionId)));
    }

    public Subscription getSelectedSubscription(String subscriptionId) {
        return getSelectedSubscriptions().stream()
            .filter(s -> StringUtils.equalsIgnoreCase(subscriptionId, s.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find a selected subscription with id '%s'", subscriptionId)));
    }

    @Override
    public List<Subscription> getSelectedSubscriptions() {
        return this.getSubscriptions().stream().filter(Subscription::isSelected).collect(Collectors.toList());
    }

    @Nonnull
    public List<String> getTenantIds() {
        return getSubscriptions().stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList());
    }

    public String getPortalUrl() {
        return AzureEnvironmentUtils.getPortalUrl(this.getEnvironment());
    }

    public AzureEnvironment getEnvironment() {
        return Azure.az(AzureCloud.class).getOrDefault();
    }

    public boolean isLoggedInCompletely() {
        return isLoggedIn() && CollectionUtils.isNotEmpty(this.getSelectedSubscriptions());
    }

    public boolean isLoggedIn() {
        return Objects.nonNull(this.defaultTokenCredential) && CollectionUtils.isNotEmpty(this.subscriptions);
    }

    public boolean isSubscriptionsSelected() {
        return isLoggedInCompletely();
    }

    @Nullable
    protected TokenCachePersistenceOptions getPersistenceOptions() {
        return isPersistenceEnabled() ? PERSISTENCE_OPTIONS : null;
    }

    private static ResourceManager.Configurable configureAzure() {
        // disable retry for getting tenant and subscriptions
        final String userAgent = Azure.az().config().getUserAgent();
        return ResourceManager.configure()
            .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
            .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
            .withRetryPolicy(new RetryPolicy(new FixedDelay(0, Duration.ofSeconds(0))));
    }

    @Override
    public String toString() {
        final List<String> details = new ArrayList<>();
        final List<Subscription> selectedSubscriptions = getSelectedSubscriptions();
        final String username = this.getUsername();
        if (getAuthType() != null) {
            details.add(String.format("Auth type: %s", TextUtils.cyan(getAuthType().toString())));
        }
        if (CollectionUtils.isNotEmpty(selectedSubscriptions)) {
            if (selectedSubscriptions.size() == 1) {
                details.add(String.format("Default subscription: %s(%s)", TextUtils.cyan(selectedSubscriptions.get(0).getName()),
                    TextUtils.cyan(selectedSubscriptions.get(0).getId())));
            }
        }
        if (StringUtils.isNotEmpty(username)) {
            details.add(String.format("Username: %s", TextUtils.cyan(username)));
        }

        return StringUtils.join(details.toArray(), "\n");
    }

    @RequiredArgsConstructor
    private static class TenantTokenCredential implements TokenCredential {
        // cache for different resources on the same tenant
        private final Map<String, SimpleTokenCache> resourceTokenCache = new ConcurrentHashMap<>();
        private final String tenantId;
        private final TokenCredential defaultCredential;

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext request) {
            request.setTenantId(StringUtils.firstNonBlank(request.getTenantId(), this.tenantId));
            // final String resource = ScopeUtil.scopesToResource(request.getScopes());
            // final Function<String, SimpleTokenCache> func = (ignore) -> new SimpleTokenCache(() -> defaultCredential.getToken(request));
            // return resourceTokenCache.computeIfAbsent(resource, func).getToken();
            return defaultCredential.getToken(request);
        }
    }
}
