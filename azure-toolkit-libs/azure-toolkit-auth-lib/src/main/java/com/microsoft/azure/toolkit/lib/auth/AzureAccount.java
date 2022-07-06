/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.resources.models.Location;
import com.azure.resourcemanager.resources.models.RegionType;
import com.azure.resourcemanager.resources.models.Subscription;
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
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription.getResourceManager;

public class AzureAccount implements IAzureAccount {
    private final AtomicReference<Account> accountRef = new AtomicReference<>();

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.accountRef.get())
            .orElseThrow(() -> new AzureToolkitAuthenticationException("you are not signed-in."));
    }

    public Account login(@Nonnull AuthType type) {
        return login(new AuthConfiguration(type), false);
    }

    public Account login(@Nonnull AuthConfiguration config) {
        return login(config, false);
    }

    public Account login(@Nonnull AuthConfiguration config, boolean enablePersistence) {
        // TODO: azure environment/cloud should be set from azure configuration before login.
        if (this.isLoggedIn()) {
            AzureMessager.getMessager().warning("You have already logged in!");
        }
        AzureEnvironment env = Azure.az(AzureCloud.class).getOrDefault();
        if (Objects.nonNull(config.getEnvironment()) && env != config.getEnvironment()) {
            String msg = String.format("you have switched to Azure Cloud '%s' since the last time you signed in.", AzureEnvironmentUtils.getCloudName(env));
            throw new AzureToolkitAuthenticationException(msg);
        }
        AuthType type = config.getType();
        final List<String> selected = config.getSelectedSubscriptions();
        final boolean restoring = CollectionUtils.isNotEmpty(selected);
        final Account account;
        if (type == AuthType.AUTO) {
            account = this.getAutoAccount(config);
            if (account.getAuthType() == AuthType.OAUTH2 || account.getAuthType() == AuthType.DEVICE_CODE) {
                Log.prompt(String.format("Auth type: %s", TextUtils.cyan(account.getAuthType().toString())));
            }
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
        AzureEventBus.emit("account.logging_in.type", config.getType());
        account.login();
        if (restoring) {
            if (StringUtils.isNotBlank(config.getUsername()) && !StringUtils.equalsIgnoreCase(account.getUsername(), config.getUsername())) {
                String msg = String.format("you have changed the account from '%s' to '%s' since the last time you signed in.",
                    config.getUsername(), account.getUsername());
                throw new AzureToolkitAuthenticationException(msg);
            }
            account.setSelectedSubscriptions(selected);
        }
        if (this.accountRef.compareAndSet(null, account)) {
            AzureEventBus.emit("account.logged_in.account", account);
        }
        return account;
    }

    @Nonnull
    private Account getAutoAccount(@Nonnull AuthConfiguration config) {
        final ArrayList<Account> candidates = new ArrayList<>(8);
        candidates.add(new ServicePrincipalAccount(config));
        // candidates.add(new SharedTokenCacheAccount(config));
        candidates.add(new ManagedIdentityAccount(config));
        candidates.add(new AzureCliAccount(config));
        candidates.add(new OAuthAccount(config));
        for (Account candidate : candidates) {
            if (candidate.checkAvailable()) {
                return candidate;
            }
        }
        return new DeviceCodeAccount(config);
    }

    public void logout() {
        final Account oldAccount = this.accountRef.getAndSet(null);
        if (Objects.nonNull(oldAccount)) {
            oldAccount.logout();
            AzureEventBus.emit("account.logged_out.account", oldAccount);
        }
    }

    public boolean isLoggedIn() {
        return Optional.ofNullable(this.accountRef.get()).map(Account::isLoggedInCompletely).isPresent();
    }

    public Account getAccount() {
        return this.accountRef.get();
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
    private Subscription getSubscription(String subscriptionId) {
        return getResourceManager(subscriptionId).subscriptions().getById(subscriptionId);
    }
}
