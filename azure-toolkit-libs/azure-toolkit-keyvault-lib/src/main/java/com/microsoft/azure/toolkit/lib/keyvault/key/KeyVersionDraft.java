/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.key;

import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.models.CreateEcKeyOptions;
import com.azure.security.keyvault.keys.models.CreateKeyOptions;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyType;
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
    public static final String KEY_CREATION_FORBIDDEN_MESSAGE = "failed to create key %s, access denied";
    public static final String KEY_CREATION_FAILED_MESSAGE = "failed to create key %s, an unexpected error occurred";
    public static final String KEY_UPDATE_FORBIDDEN_MESSAGE = "failed to create key %s, access denied";
    public static final String KEY_UPDATE_FAILED_MESSAGE = "failed to create key %s, an unexpected error occurred";

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
    @AzureOperation(name = "azure/keyvault.create_key_version.version", params = {"this.getName()"})
    public KeyProperties createResourceInAzure() {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_key_version.version", params = {"this.getName()"})
    public KeyProperties updateResourceInAzure(@Nonnull KeyProperties origin) {
        final KeyAsyncClient client = Objects.requireNonNull(getKeyVault().getKeyClient());
        final KeyDraft.Config config = ensureConfig();
        final Boolean isEnabled = config.getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            return updateKeyVersion(client, origin, config);
        }
        return origin;
    }

    public static KeyProperties createKeyVersion(@Nonnull final KeyAsyncClient keyClient, @Nonnull final KeyDraft.Config config) {
        try {
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
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(KEY_CREATION_FORBIDDEN_MESSAGE, config.getName()));
            }
            throw new AzureToolkitRuntimeException(String.format(KEY_CREATION_FAILED_MESSAGE, config.getName()));
        }
    }

    public static KeyProperties updateKeyVersion(@Nonnull final KeyAsyncClient client,
                                                 @Nonnull final KeyProperties origin,
                                                 @Nonnull final KeyDraft.Config config) {
        try {
            origin.setEnabled(config.getEnabled());
            return Objects.requireNonNull(client.updateKeyProperties(origin).block(), "failed to update secret").getProperties();
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(KEY_CREATION_FORBIDDEN_MESSAGE, config.getName()));
            }
            throw new AzureToolkitRuntimeException(String.format(KEY_CREATION_FAILED_MESSAGE, config.getName()));
        }
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

