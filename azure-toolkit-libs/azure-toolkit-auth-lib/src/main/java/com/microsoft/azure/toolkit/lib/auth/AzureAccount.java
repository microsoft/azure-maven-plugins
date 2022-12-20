/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.resources.models.Location;
import com.azure.resourcemanager.resources.models.RegionType;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.cli.AzureCliAccount;
import com.microsoft.azure.toolkit.lib.auth.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.managedidentity.ManagedIdentityAccount;
import com.microsoft.azure.toolkit.lib.auth.oauth.OAuthAccount;
import com.microsoft.azure.toolkit.lib.auth.serviceprincipal.ServicePrincipalAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription.getResourceManager;

public class AzureAccount implements IAzureAccount {
    @Nullable
    private AtomicReference<Account> accountRef;

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.accountRef).map(AtomicReference::get)
            .orElseThrow(() -> new AzureToolkitAuthenticationException("you are not signed-in."));
    }

    public Account login(@Nonnull AuthType type) {
        return login(new AuthConfiguration(type), true);
    }

    public Account login(@Nonnull AuthConfiguration config) {
        return login(config, true);
    }

    public Account login(@Nonnull Account account) {
        if (this.isLoggedIn()) {
            AzureMessager.getMessager().warning("You have already logged in!");
            return this.account();
        }
        if (account.getType() == AuthType.OAUTH2 || account.getType() == AuthType.DEVICE_CODE) {
            Log.prompt(String.format("Auth type: %s", TextUtils.cyan(account.getType().name())));
        }
        this.accountRef = new AtomicReference<>();
        AzureEventBus.emit("account.logging_in.type", account.getType());
        account.login();
        if (this.accountRef.compareAndSet(null, account)) {
            AzureEventBus.emit("account.logged_in.account", account);
        }
        return account;
    }

    @AzureOperation(name = "internal/account.login.type", params = {"config.getType()"})
    public synchronized Account login(@Nonnull AuthConfiguration config, boolean enablePersistence) {
        // TODO: azure environment/cloud should be set from azure configuration before login.
        if (this.isLoggedIn()) {
            AzureMessager.getMessager().warning("You have already logged in!");
            return this.account();
        }
        final AzureEnvironment env = Azure.az(AzureCloud.class).getOrDefault();
        final AzureEnvironment configEnv = AzureEnvironmentUtils.stringToAzureEnvironment(config.getEnvironment());
        if (Objects.nonNull(configEnv) && env != configEnv) {
            String msg = String.format("you have switched to Azure Cloud '%s' since the last time you signed in.", AzureEnvironmentUtils.getCloudName(env));
            this.logout();
            throw new AzureToolkitAuthenticationException(msg);
        }
        final AuthType type = config.getType();
        OperationContext.current().setTelemetryProperty("authType", type.name());
        OperationContext.current().setTelemetryProperty("azureEnvironment", AzureEnvironmentUtils.azureEnvironmentToString(env));
        final List<String> selected = config.getSelectedSubscriptions();
        final boolean restoring = CollectionUtils.isNotEmpty(selected);
        final Account account;
        if (type == AuthType.AUTO) {
            account = this.getAutoAccount(config);
        } else if (type == AuthType.SERVICE_PRINCIPAL) {
            account = new ServicePrincipalAccount(config);
        } else if (type == AuthType.MANAGED_IDENTITY) {
            account = new ManagedIdentityAccount(config);
        } else if (type == AuthType.AZURE_CLI) {
            account = new AzureCliAccount(config);
        } else if (type == AuthType.OAUTH2) {
            account = restoring ? new SharedTokenCacheAccount(config) : new OAuthAccount(config);
        } else if (type == AuthType.DEVICE_CODE) {
            account = restoring ? new SharedTokenCacheAccount(config) : new DeviceCodeAccount(config);
        } else {
            throw new AzureToolkitRuntimeException(String.format("Unsupported auth type '%s'", type));
        }
        account.setPersistenceEnabled(enablePersistence);
        if (account.getType() == AuthType.OAUTH2 || account.getType() == AuthType.DEVICE_CODE) {
            Log.prompt(String.format("Auth type: %s", TextUtils.cyan(account.getType().name())));
        }
        this.accountRef = new AtomicReference<>();
        AzureEventBus.emit("account.logging_in.type", account.getType());
        try {
            account.login();
        } catch (Throwable t) {
            AzureEventBus.emit("account.failed_logging_in.type", account.getType());
            throw t;
        }
        if (this.accountRef.compareAndSet(null, account)) {
            if (restoring) {
                account.setSelectedSubscriptions(selected);
            }
            AzureEventBus.emit("account.logged_in.account", account);
        }
        return account;
    }

    public Account getAutoAccount() {
        return this.getAutoAccount(new AuthConfiguration(AuthType.AUTO));
    }

    @Nonnull
    private Account getAutoAccount(@Nonnull AuthConfiguration config) {
        final List<Account> candidates = new ArrayList<>(8);
        candidates.add(new ServicePrincipalAccount(config));
        // candidates.add(new SharedTokenCacheAccount(config));
        candidates.add(new ManagedIdentityAccount(config));
        candidates.add(new AzureCliAccount(config));
        candidates.add(new OAuthAccount(config));
        final Account account = candidates.stream().parallel().filter(Account::checkAvailable).findFirst().orElseGet(() -> new DeviceCodeAccount(config));
        config.setType(account.getType());
        return account;
    }

    public synchronized void logout() {
        final Account oldAccount = Optional.ofNullable(this.accountRef).map(r -> r.getAndSet(null)).orElse(null);
        if (Objects.nonNull(oldAccount)) {
            oldAccount.logout();
            this.accountRef = null;
            AzureEventBus.emit("account.logged_out.account", oldAccount);
        }
    }

    public boolean isLoggedIn() {
        return Optional.ofNullable(this.accountRef).map(AtomicReference::get).map(Account::isLoggedInCompletely).isPresent();
    }

    public boolean isLoggingIn() {
        return Objects.nonNull(this.accountRef) && Objects.isNull(this.accountRef.get());
    }

    @Nullable
    public Account getAccount() {
        return Optional.ofNullable(this.accountRef).map(AtomicReference::get).orElse(null);
    }

    @Override
    public String getName() {
        return "Microsoft.Account";
    }

    @Override
    public void refresh() {
        // do nothing
    }

    /**
     * see doc for: az account list-locations -o table
     */
    @Cacheable(cacheName = "subscriptions/{}/regions", key = "$subscriptionId")
    public List<Region> listRegions(String subscriptionId) {
        return getSubscription(subscriptionId).listLocations().stream()
            .filter(l -> l.regionType() == RegionType.PHYSICAL) // use distinct since com.azure.core.management.Region impels equals
            .map(Location::region).distinct()
            .map(region -> Region.fromName(region.name())).collect(Collectors.toList());
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

    // todo: share codes with other library which leverage track2 mgmt sdk
    @Cacheable(cacheName = "subscriptions/{}", key = "$subscriptionId")
    private com.azure.resourcemanager.resources.models.Subscription getSubscription(String subscriptionId) {
        return getResourceManager(subscriptionId).subscriptions().getById(subscriptionId);
    }
}
