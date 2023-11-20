/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.models.CreateEcKeyOptions;
import com.azure.security.keyvault.keys.models.CreateKeyOptions;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyCurveName;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyType;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.Secret;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

public class KeyDraft extends Key implements AzResource.Draft<Key, KeyProperties> {

    @Getter
    private Key origin;

    @Setter
    private Config config;

    protected KeyDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull KeyModule module) {
        super(name, resourceGroup, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    public KeyProperties createResourceInAzure() {
        final KeyAsyncClient keyClient = getKeyVault().getKeyClient();
        return createOrUpdateKey(keyClient, ensureConfig());
    }

    @Nonnull
    @Override
    public KeyProperties updateResourceInAzure(@Nonnull KeyProperties origin) {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private Config ensureConfig() {
        return Optional.ofNullable(config).orElseGet(Config::new);
    }

    public static KeyProperties createOrUpdateKey(@Nonnull final KeyAsyncClient keyClient, @Nonnull final Config config) {
        final KeyType keyType = Objects.requireNonNull(config.getKeyType(), "Type is required to create a key");
        final CreateKeyOptions options;
        if (keyType == KeyType.RSA) {
            options = new CreateRsaKeyOptions(config.getName())
                .setKeySize(config.getRasKeySize());
        } else if (keyType == KeyType.EC) {
            options = new CreateEcKeyOptions(config.getName())
                .setCurveName(config.getCurveName());
        } else {
            throw new AzureToolkitRuntimeException("Not support key type: " + keyType);
        }
        options.setEnabled(config.getEnabled());
        options.setNotBefore(config.getActivationDate());
        options.setExpiresOn(config.getExpirationDate());
        return Objects.requireNonNull(keyClient.createKey(options).block(), "failed to create key").getProperties();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private String name;
        private KeyType keyType;
        private OffsetDateTime activationDate;
        private OffsetDateTime expirationDate;
        private Integer rasKeySize;
        private KeyCurveName curveName;
        private Boolean enabled = Boolean.TRUE;
    }
}
