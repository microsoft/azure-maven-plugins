/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.SharedTokenCacheCredential;
import com.azure.identity.SharedTokenCacheCredentialBuilder;
import com.azure.identity.TokenCachePersistenceOptions;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.policy.ProviderRegistrationPolicy;
import com.azure.resourcemanager.resources.models.Location;
import com.azure.resourcemanager.resources.models.Providers;
import com.azure.resourcemanager.resources.models.RegionType;
import com.azure.resourcemanager.resources.models.Subscription;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.core.azurecli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.core.oauth.OAuthAccount;
import com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal.ServicePrincipalAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AzureAccount implements IAzureAccount {

    @Setter(AccessLevel.PACKAGE)
    private Account account;

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
                .orElseThrow(() -> new AzureToolkitAuthenticationException("you are not signed-in."));
    }

    public Account account(@Nonnull AccountEntity accountEntity) {
        return restoreLogin(accountEntity).block();
    }

    public List<Account> accounts() {
        return Flux.fromIterable(buildAccountMap().values()).map(Supplier::get).collectList().block();
    }

    public AzureAccount login(@Nonnull AuthType type) {
        return login(type, false);
    }

    public AzureAccount login(@Nonnull Account targetAccount) {
        return login(targetAccount, false);
    }

    public AzureAccount login(@Nonnull AuthConfiguration auth) {
        return login(auth, false);
    }

    public AzureAccount login(@Nonnull AuthType type, boolean enablePersistence) {
        if (type == AuthType.DEVICE_CODE) {
            throw new IllegalArgumentException("You shall not call login in sync mode for device code login, you need to call loginAsync instead.");
        }
        return finishLogin(loginAsync(type, enablePersistence));
    }

    public AzureAccount login(@Nonnull Account targetAccount, boolean enablePersistence) {
        if (targetAccount.getAuthType() == AuthType.DEVICE_CODE) {
            throw new IllegalArgumentException("You shall not call login in sync mode for device code login, you need to call loginAsync instead.");
        }
        return finishLogin(loginAsync(targetAccount, enablePersistence));
    }

    public AzureAccount login(@Nonnull AuthConfiguration auth, boolean enablePersistence) {
        if (auth.getType() == AuthType.DEVICE_CODE) {
            throw new IllegalArgumentException("You shall not call login in sync mode for device code login, you need to call loginAsync instead.");
        }
        return finishLogin(loginAsync(auth, enablePersistence));
    }

    public void logout() {
        if (this.account != null) {
            Account tempAccount = this.account;
            this.account = null;
            tempAccount.logout();
        }
    }

    private Mono<Account> restoreLogin(@Nonnull AccountEntity accountEntity) {
        Preconditions.checkNotNull(accountEntity.getEnvironment(), "Azure environment for account entity is required.");
        Preconditions.checkNotNull(accountEntity.getType(), "Auth type for account entity is required.");
        Account target;
        if (Arrays.asList(AuthType.DEVICE_CODE, AuthType.OAUTH2).contains(accountEntity.getType())) {
            AzureEnvironmentUtils.setupAzureEnvironment(accountEntity.getEnvironment());
            SharedTokenCacheCredentialBuilder builder = new SharedTokenCacheCredentialBuilder();
            SharedTokenCacheCredential credential = builder
                    .tokenCachePersistenceOptions(new TokenCachePersistenceOptions().setName(Account.TOOLKIT_TOKEN_CACHE_NAME))
                    // default tenant id in azure identity is organizations
                    // see https://github.com/Azure/azure-sdk-for-java/blob/026664ea871586e681ab674e0332b6cc2352c655
                    // /sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClient.java#L139
                    .username(accountEntity.getEmail()).tenantId(accountEntity.getTenantIds() == null ? "organizations" :
                            accountEntity.getTenantIds().get(0)).clientId(accountEntity.getClientId())
                    .build();

            target = new SimpleAccount(accountEntity, credential);
        } else if (Arrays.asList(AuthType.VSCODE, AuthType.AZURE_CLI).contains(accountEntity.getType())) {
            target = buildAccountMap().get(accountEntity.getType()).get();
        } else {
            return Mono.error(new AzureToolkitAuthenticationException(String.format("login for auth type '%s' cannot be restored.", accountEntity.getType())));
        }
        return target.login().map(ac -> {
            if (ac.getEnvironment() != accountEntity.getEnvironment()) {
                throw new AzureToolkitAuthenticationException(
                        String.format("you have changed the azure cloud to '%s' for auth type: '%s' since last time you signed in.",
                                AzureEnvironmentUtils.getCloudNameForAzureCli(ac.getEnvironment()), accountEntity.getType()));
            }
            if (!StringUtils.equalsIgnoreCase(ac.entity.getEmail(), accountEntity.getEmail())) {
                throw new AzureToolkitAuthenticationException(
                        String.format("you have changed the account from '%s' to '%s' since last time you signed in.",
                                accountEntity.getEmail(), ac.entity.getEmail()));
            }
            return ac;
        }).doOnSuccess(this::setAccount);
    }

    static class SimpleAccount extends Account {
        private final TokenCredential credential;

        public SimpleAccount(@Nonnull AccountEntity accountEntity, @Nonnull TokenCredential credential) {
            Preconditions.checkNotNull(accountEntity.getEnvironment(), "Azure environment for account entity is required.");
            Preconditions.checkNotNull(accountEntity.getType(), "Auth type for account entity is required.");
            this.entity = new AccountEntity();
            this.entity.setClientId(accountEntity.getClientId());
            this.entity.setType(accountEntity.getType());
            this.entity.setEmail(accountEntity.getEmail());
            this.entity.setEnvironment(accountEntity.getEnvironment());
            this.credential = credential;
        }
        protected Mono<TokenCredentialManager> createTokenCredentialManager() {
            AzureEnvironment env = this.entity.getEnvironment();
            return RefreshTokenTokenCredentialManager.createTokenCredentialManager(env, getClientId(), createCredential());
        }

        private TokenCredential createCredential() {
            return credential;
        }

        @Override
        public AuthType getAuthType() {
            return this.entity.getType();
        }

        @Override
        protected String getClientId() {
            return this.entity.getClientId();
        }

        @Override
        protected Mono<Boolean> preLoginCheck() {
            return Mono.just(true);
        }
    }

    public Mono<Account> loginAsync(@Nonnull AuthType type, boolean enablePersistence) {
        Objects.requireNonNull(type, "Please specify auth type in auth configuration.");
        AuthConfiguration auth = new AuthConfiguration();
        auth.setType(type);
        return loginAsync(auth, enablePersistence);
    }

    public Mono<Account> loginAsync(@Nonnull AuthConfiguration auth, boolean enablePersistence) {
        Objects.requireNonNull(auth, "Auth configuration is required for login.");
        Objects.requireNonNull(auth.getType(), "Auth type is required for login.");
        Preconditions.checkArgument(auth.getType() != AuthType.AUTO, "Auth type 'auto' is illegal for login.");
        if (auth.getEnvironment() != null) {
            Azure.az(AzureCloud.class).set(auth.getEnvironment());
        }
        AuthType type = auth.getType();
        final Account targetAccount;
        if (auth.getType() == AuthType.SERVICE_PRINCIPAL) {
            targetAccount = new ServicePrincipalAccount(auth);
        } else {
            Map<AuthType, Supplier<Account>> accountByType = buildAccountMap();
            if (!accountByType.containsKey(type)) {
                return Mono.error(new LoginFailureException(String.format("Unsupported auth type '%s', supported values are: %s.",
                        type, accountByType.keySet().stream().map(Object::toString).map(StringUtils::lowerCase).collect(Collectors.joining(", ")))));
            }
            targetAccount = accountByType.get(type).get();
        }

        return loginAsync(targetAccount, enablePersistence);
    }

    public Mono<Account> loginAsync(Account targetAccount, boolean enablePersistence) {
        Objects.requireNonNull(targetAccount, "Please specify account to login.");
        targetAccount.setEnablePersistence(enablePersistence);
        return targetAccount.login();
    }

    /**
     * see doc for: az account list-locations -o table
     */
    @Cacheable(cacheName = "Regions", key = "$subscriptionId")
    public List<Region> listRegions(String subscriptionId) {
        return getSubscription(subscriptionId).listLocations().stream()
                .filter(l -> l.regionType() == RegionType.PHYSICAL) // use distinct since com.azure.core.management.Region impls equals
                .map(Location::region).distinct().map(AzureAccount::toRegion).collect(Collectors.toList());
    }

    /**
     * see doc for: az account list-locations -o table
     */
    public List<Region> listRegions() {
        return Flux.fromIterable(Azure.az(IAzureAccount.class).account().getSelectedSubscriptions())
                .parallel().map(com.microsoft.azure.toolkit.lib.common.model.Subscription::getId)
                .map(this::listRegions)
                .sequential().collectList()
                .map(regionSet -> regionSet.stream()
                .flatMap(Collection::stream)
                .filter(Utils.distinctByKey(region -> StringUtils.lowerCase(region.getLabel()))) // cannot distinct since Region doesn't impl equals
                .collect(Collectors.toList())).block();
    }

    private static Region toRegion(com.azure.core.management.Region region) {
        return Optional.of(Region.fromName(region.name())).orElseGet(() -> new Region(region.name(), region.label() + "*"));
    }

    private AzureAccount finishLogin(Mono<Account> mono) {
        try {
            mono.flatMap(Account::continueLogin).block();
            return this;
        } catch (Throwable ex) {
            throw new AzureToolkitAuthenticationException("encountering error: " + ex.getMessage());
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


    // todo: share codes with other library which leverage track2 mgmt sdk
    @Cacheable(cacheName = "Subscription", key = "$subscriptionId")
    private Subscription getSubscription(String subscriptionId) {
        return getResourceManager(subscriptionId).subscriptions().getById(subscriptionId);
    }

    @Cacheable(cacheName = "resource/{}/manager", key = "$subscriptionId")
    private ResourceManager getResourceManager(String subscriptionId) {
        // make sure it is signed in.
        account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logDetailLevel = config.getLogLevel() == null ?
                HttpLogDetailLevel.NONE : HttpLogDetailLevel.valueOf(config.getLogLevel());
        final AzureProfile azureProfile = new AzureProfile(account.getEnvironment());

        final Providers providers = ResourceManager.configure()
            .withPolicy(getUserAgentPolicy(userAgent))
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
            .withSubscription(subscriptionId).providers();
        return ResourceManager.configure()
                .withLogLevel(logDetailLevel)
                .withPolicy(getUserAgentPolicy(userAgent)) // set user agent with policy
            .withPolicy(new ProviderRegistrationPolicy(providers)) // add policy to auto register resource providers
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile)
                .withSubscription(subscriptionId);
    }

    private HttpPipelinePolicy getUserAgentPolicy(String userAgent) {
        return (httpPipelineCallContext, httpPipelineNextPolicy) -> {
            final String previousUserAgent = httpPipelineCallContext.getHttpRequest().getHeaders().getValue("User-Agent");
            httpPipelineCallContext.getHttpRequest().setHeader("User-Agent", String.format("%s %s", userAgent, previousUserAgent));
            return httpPipelineNextPolicy.process();
        };
    }
}
