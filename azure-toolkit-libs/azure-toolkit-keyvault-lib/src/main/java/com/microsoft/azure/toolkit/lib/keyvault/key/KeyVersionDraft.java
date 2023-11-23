/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.key;

import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class KeyVersionDraft extends KeyVersion
    implements AzResource.Draft<KeyVersion, KeyProperties> {

    @Getter
    private final KeyVersion origin;

    @Setter
    private KeyDraft.Config config;

    protected KeyVersionDraft(@Nonnull KeyVersion origin) {
        super(origin);
        this.origin = origin;
    }


    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvaults.create_key_version.version", params = {"this.getName()"})
    public KeyProperties createResourceInAzure() {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvaults.update_key_version.version", params = {"this.getName()"})
    public KeyProperties updateResourceInAzure(@Nonnull KeyProperties origin) {
        final KeyAsyncClient client = Objects.requireNonNull(getKeyVault().getKeyClient());
        final Boolean isEnabled = ensureConfig().getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            origin.setEnabled(isEnabled);
            return Objects.requireNonNull(client.updateKeyProperties(origin).block(), "failed to update secret").getProperties();
        }
        return origin;
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private KeyDraft.Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(KeyDraft.Config::new);
        return this.config;
    }

    public void setEnabled(boolean b) {
        ensureConfig().setEnabled(b);
    }
}

