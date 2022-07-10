/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.devicecode;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class DeviceCodeAccount extends Account {
    @Getter
    private final AuthType type = AuthType.DEVICE_CODE;

    public DeviceCodeAccount(@Nonnull AuthConfiguration config) {
        super(config);
        if (Objects.isNull(config.getDeviceCodeConsumer())) {
            throw new AzureToolkitRuntimeException("device code consumer is not configured.");
        }
    }

    @Override
    public boolean checkAvailable() {
        return true;
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        final AuthConfiguration config = this.getConfig();
        if (Optional.ofNullable(config.getExecutorService()).filter(ExecutorService::isShutdown).isPresent()) {
            throw new AzureToolkitAuthenticationException("device login twice is forbidden.");
        }
        return new DeviceCodeCredentialBuilder()
            .clientId(this.getClientId())
            .tenantId(config.getTenant())
            .tokenCachePersistenceOptions(this.getPersistenceOptions())
            .executorService(config.getExecutorService())
            .challengeConsumer(config.getDeviceCodeConsumer())
            .build();
    }
}
