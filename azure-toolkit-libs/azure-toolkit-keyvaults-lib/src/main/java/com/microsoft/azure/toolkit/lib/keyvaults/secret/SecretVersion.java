/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SecretVersion extends AbstractAzResource<SecretVersion, Secret, SecretProperties> {

    protected SecretVersion(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull SecretVersionModule module) {
        super(name, resourceGroupName, module);
    }

    protected SecretVersion(@Nonnull SecretVersion origin) {
        super(origin);
    }

    protected SecretVersion(@Nonnull SecretProperties remote, @Nonnull SecretVersionModule module) {
        super(remote.getVersion(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull SecretProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::isEnabled).orElse(false);
    }

    @Nullable
    public String getVersion() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::getVersion).orElse(null);
    }

    public boolean isCurrentVersion() {
        return StringUtils.equals(getVersion(), getParent().getCurrentVersionId());
    }

    @Nullable
    public String getSecretValue() {
        return Optional.ofNullable(getSecret()).map(KeyVaultSecret::getValue).orElse(null);
    }

    @Nullable
    public KeyVaultSecret getSecret() {
        final Secret secret = getParent();
        final KeyVault keyVault = secret.getParent();
        return Optional.ofNullable(keyVault.getSecretClient())
            .map(client -> client.getSecret(secret.getName(), getVersion()).block())
            .orElse(null);
    }

    @Nullable
    public SecretProperties getProperties() {
        return getRemote();
    }
}