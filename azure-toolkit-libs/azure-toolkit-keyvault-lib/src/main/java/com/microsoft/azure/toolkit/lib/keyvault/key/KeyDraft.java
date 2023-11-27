/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.key;

import com.azure.security.keyvault.keys.models.KeyCurveName;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyType;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
    @AzureOperation(name = "azure/keyvault.create_key.key", params = {"this.getName()"})
    public KeyProperties createResourceInAzure() {
        return KeyVersionDraft.createKeyVersion(getKeyVault(), ensureConfig());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_key.key", params = {"this.getName()"})
    public KeyProperties updateResourceInAzure(@Nonnull KeyProperties origin) {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(KeyDraft.Config::new);
        return this.config;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private String name;
        private KeyType keyType;
        private Integer rasKeySize;
        private KeyCurveName curveName;
        private Boolean enabled = Boolean.TRUE;
        private Boolean enableActivationDate = Boolean.FALSE;
        private OffsetDateTime activationDate;
        private Boolean enableExpirationDate = Boolean.FALSE;
        private OffsetDateTime expirationDate;
    }
}
