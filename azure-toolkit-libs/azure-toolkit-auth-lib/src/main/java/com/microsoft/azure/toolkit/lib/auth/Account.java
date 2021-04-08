/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.http.policy.FixedDelay;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.implementation.MsalToken;
import com.azure.identity.implementation.util.IdentityConstants;
import com.azure.identity.implementation.util.ScopeUtil;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Tenant;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.auth.core.refresktoken.RefreshTokenTenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class Account implements IAccount {
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

    protected Mono<Account> authenticate() {
        if (!this.checkAvailable()) {
            return Mono.error(new AzureToolkitAuthenticationException("Cannot find credential using auth method: " + this.getMethod()));
        }
        return Mono.fromCallable(this::createTokenCredential).flatMap(this::verifyAndGetTenantId).map(tenantAndToken -> {
            if (tenantAndToken.getT2() instanceof MsalToken) {
                if (StringUtils.isBlank(this.entity.getEmail())) {
                    this.entity.setEmail(getEmailFromMsalToken(tenantAndToken.getT2()));
                }
                String refreshToken = getRefreshTokenFromMsalToken(tenantAndToken.getT2());
                if (StringUtils.isNotEmpty(refreshToken)) {
                    AzureEnvironment env = ObjectUtils.firstNonNull(this.entity.getEnvironment(), Azure.az(AzureCloud.class).getOrDefault());
                    String authority = AzureEnvironmentUtils.getAuthority(env);
                    //TODO: when vscode/vs login is added, make sure to change it here
                    this.entity.setTenantCredential(new RefreshTokenTenantCredential(
                            authority, IdentityConstants.DEVELOPER_SINGLE_SIGN_ON_ID, refreshToken
                    ));
                }
            }
            if (this.entity.getTenantIds() == null) {
                this.entity.setTenantIds(tenantAndToken.getT1());
            }
            this.entity.setAuthenticated(true);
            return this;
        }).flatMap(ignore -> {
            if (this.entity.getSubscriptions() == null) {
                return initializeSubscriptions().then(Mono.just(this));
            }
            return Mono.just(this);
        });
    }

    private static String getEmailFromMsalToken(AccessToken accessToken) {
        IAuthenticationResult result = ((MsalToken) accessToken).getAuthenticationResult();

        if (result != null && result.account() != null) {
            return result.account().username();
        }
        return null;
    }

    private String getRefreshTokenFromMsalToken(AccessToken accessToken) {
        IAuthenticationResult result = ((MsalToken) accessToken).getAuthenticationResult();
        if (result == null) {
            return null;
        }

        String refreshTokenFromResult;
        try {
            refreshTokenFromResult = (String) FieldUtils.readField(result, "refreshToken", true);
        } catch (IllegalAccessException e) {
            throw new AzureToolkitAuthenticationException("Cannot read refreshToken from IAuthenticationResult.");
        }

        return refreshTokenFromResult;
    }

    private Mono<Tuple2<List<String>, AccessToken>> verifyAndGetTenantId(TokenCredential credential) {
        AzureEnvironment env = ObjectUtils.firstNonNull(this.entity.getEnvironment(), Azure.az(AzureCloud.class).getOrDefault());
        TokenRequestContext tokenRequestContext = new TokenRequestContext()
                .addScopes(ScopeUtil.resourceToScopes(env.getManagementEndpoint()));
        Mono<AccessToken> mono = credential instanceof TenantCredential ?
                ((TenantCredential) credential).getAccessToken(null, tokenRequestContext) :
                credential.getToken(tokenRequestContext);

        return mono.flatMap(accessToken ->
                Mono.zip(AzureResourceManager.authenticate(request -> Mono.just(accessToken), new AzureProfile(env))
                        .tenants().listAsync().map(Tenant::tenantId).collectList(), Mono.just(accessToken)));
    }

    protected Mono<Void> initializeSubscriptions() {
        return listSubscriptions(entity.getTenantIds(), entity.getTenantCredential()).doOnSuccess(subscriptions -> {
            entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));
            entity.setSubscriptions(subscriptions);
            selectSubscriptionInner(subscriptions, this.entity.getSelectedSubscriptionIds());

            // no subscriptions are selected, selected all
            if (!subscriptions.isEmpty() && subscriptions.stream().noneMatch(Subscription::isSelected)) {
                subscriptions.forEach(s -> s.setSelected(true));
            }
        }).then();
    }

    private Mono<List<Subscription>> listSubscriptions(List<String> tenantIds, TenantCredential credential) {
        AzureProfile azureProfile = new AzureProfile(ObjectUtils.firstNonNull(this.entity.getEnvironment(), Azure.az(AzureCloud.class).getOrDefault()));
        // use map to re-dup subs
        final Map<String, Subscription> subscriptionMap = new ConcurrentHashMap<>();
        Map<String, Throwable> throwableList = new ConcurrentHashMap<>();
        return Flux.fromIterable(tenantIds).parallel().runOn(Schedulers.boundedElastic())
                .flatMap(t -> listSubscriptions(azureProfile, t, credential.createTokenCredential(t), throwableList)).sequential().collectList()
                .map(subscriptionsSet -> {
                    for (List<Subscription> subscriptionsOnTenant : subscriptionsSet) {
                        for (Subscription subscription : subscriptionsOnTenant) {
                            String key = StringUtils.lowerCase(subscription.getId());
                            subscriptionMap.putIfAbsent(key, subscription);
                        }
                    }
                    for (Map.Entry<String, Throwable> tenantError : throwableList.entrySet()) {
                        Throwable ex = tenantError.getValue();
                        if ((ExceptionUtils.getRootCause(ex) instanceof AuthenticationException)) {
                            System.out.println("Cannot get subscriptions for tenant " + tenantError.getKey() +
                                    ", please verify you have proper permissions over this tenant.");
                        } else {
                            throw new AzureToolkitAuthenticationException(ex.getMessage());
                        }
                    }

                    return new ArrayList<>(subscriptionMap.values());
                });
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

    private static Mono<List<Subscription>> listSubscriptions(AzureProfile profile,
                                                              String tenantId,
                                                              TokenCredential credential,
                                                              Map<String, Throwable> errorsOnTenant) {
        return configureAzure().authenticate(credential, profile).subscriptions().listAsync()
                .map(s -> toSubscriptionEntity(tenantId, s)).collectList().onErrorResume(e -> {
                    errorsOnTenant.put(tenantId, e);
                    return Mono.just(new ArrayList<>());
                });
    }

    private static Subscription toSubscriptionEntity(String tenantId,
                                                     com.azure.resourcemanager.resources.models.Subscription subscription) {
        final Subscription subscriptionEntity = new Subscription();
        subscriptionEntity.setId(subscription.subscriptionId());
        subscriptionEntity.setName(subscription.displayName());
        subscriptionEntity.setTenantId(tenantId);
        return subscriptionEntity;
    }

    private static AzureResourceManager.Configurable configureAzure() {
        // disable retry for getting tenant and subscriptions
        return AzureResourceManager.configure()
                .withPolicy(createUserAgentPolicy())
                .withRetryPolicy(new RetryPolicy(new FixedDelay(0, Duration.ofSeconds(0))));
    }

    private static HttpPipelinePolicy createUserAgentPolicy() {
        final String userAgent = Azure.az().config().getUserAgent();
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }

    private void requireAuthenticated() {
        if (!this.isAuthenticated()) {
            throw new AzureToolkitAuthenticationException("Please signed in first.");
        }
    }
}
