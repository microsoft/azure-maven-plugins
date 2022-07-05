/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.devicecode;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DeviceCodeAccount extends Account {
    private static final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("azure-toolkit-auth-%d").build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, namedThreadFactory);
    @Nonnull
    private final AuthConfiguration config;

    public DeviceCodeAccount(@Nonnull AuthConfiguration config) {
        super(AuthType.DEVICE_CODE);
        this.config = config;
    }

    @Override
    public boolean checkAvailable() {
        return true;
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        if (executorService.isShutdown()) {
            throw new AzureToolkitAuthenticationException("device login twice is forbidden.");
        }
        return new DeviceCodeCredentialBuilder()
            .clientId(this.getClientId())
            .tenantId(this.config.getTenant())
            .tokenCachePersistenceOptions(this.getPersistenceOptions())
            .executorService(executorService)
            .challengeConsumer(this.config.getDeviceCodeConsumer())
            .build();
    }

    @Override
    protected void setupAfterLogin(TokenCredential defaultTokenCredential) {
        super.setupAfterLogin(defaultTokenCredential);
        this.executorService.shutdown();
    }
}
