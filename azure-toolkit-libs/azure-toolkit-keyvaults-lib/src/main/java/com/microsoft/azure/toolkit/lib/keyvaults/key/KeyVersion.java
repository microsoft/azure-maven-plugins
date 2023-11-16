/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.security.keyvault.keys.models.JsonWebKey;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyType;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KeyVersion extends AbstractAzResource<KeyVersion, Key, KeyProperties> {

    public static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    public static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    protected KeyVersion(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KeyVersionModule module) {
        super(name, resourceGroupName, module);
    }

    protected KeyVersion(@Nonnull KeyVersion origin) {
        super(origin);
    }

    protected KeyVersion(@Nonnull KeyProperties remote, @Nonnull KeyVersionModule module) {
        super(remote.getVersion(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull KeyProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::isEnabled).orElse(false);
    }

    public String getVersion() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::getVersion).orElse(null);
    }

    public boolean isCurrentVersion() {
        return StringUtils.equals(getVersion(), getParent().getCurrentVersionId());
    }

    @Nullable
    public KeyProperties getProperties() {
        return getRemote();
    }

    @Nullable
    public KeyVaultKey getSecret() {
        final Key key = getParent();
        final KeyVault keyVault = key.getParent();

        return Optional.ofNullable(keyVault.getKeyClient())
            .map(client -> client.getKey(key.getName(), getVersion()).block())
            .orElse(null);
    }

    @Nullable
    public byte[] getEncodedPublicKey() {
        final KeyVaultKey secret = getSecret();
        if (Objects.isNull(secret)) {
            return null;
        }
        final KeyType keyType = secret.getKeyType();
        final JsonWebKey key = secret.getKey();
        if (keyType == KeyType.RSA || keyType == KeyType.RSA_HSM) {
            return key.toRsa().getPublic().getEncoded();
        } else if (keyType == KeyType.EC || keyType == KeyType.EC_HSM) {
            return key.toEc().getPublic().getEncoded();
        }
        throw new AzureToolkitRuntimeException("Unsupported key type: " + keyType);
    }

    @Nullable
    public String getPublicKeyPem() {
        final byte[] encodedPublicKey = getEncodedPublicKey();
        if (Objects.isNull(encodedPublicKey)) {
            return null;
        }
        return BEGIN_PUBLIC_KEY + "\n" + Base64.getEncoder().encodeToString(encodedPublicKey) + "\n" + END_PUBLIC_KEY;
    }
}


