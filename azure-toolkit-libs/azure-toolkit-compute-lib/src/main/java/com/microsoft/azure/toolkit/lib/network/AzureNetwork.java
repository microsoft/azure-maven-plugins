/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.network.NetworkManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.network.networksecuritygroup.NetworkSecurityGroupModule;
import com.microsoft.azure.toolkit.lib.network.publicipaddress.PublicIpAddressModule;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.NetworkModule;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Optional;

@Slf4j
public class AzureNetwork extends AbstractAzService<NetworkResourceManager, NetworkManager> {
    public AzureNetwork() {
        super("Microsoft.Network");
    }

    @Nonnull
    public NetworkModule virtualNetworks(@Nonnull String subscriptionId) {
        final NetworkResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getNetworkModule();
    }

    @Nonnull
    public NetworkSecurityGroupModule networkSecurityGroups(@Nonnull String subscriptionId) {
        final NetworkResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getNetworkSecurityGroupModule();
    }

    @Nonnull
    public PublicIpAddressModule publicIpAddresses(@Nonnull String subscriptionId) {
        final NetworkResourceManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getPublicIpAddressModule();
    }

    @Nonnull
    @Override
    protected NetworkManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return NetworkManager.configure()
            .withHttpClient(AzureService.getDefaultHttpClient())
            .withLogLevel(logLevel)
            .withPolicy(AzureService.getUserAgentPolicy(userAgent)) // set user agent with policy
            .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    protected NetworkResourceManager newResource(@Nonnull NetworkManager remote) {
        return new NetworkResourceManager(remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure Network";
    }
}
