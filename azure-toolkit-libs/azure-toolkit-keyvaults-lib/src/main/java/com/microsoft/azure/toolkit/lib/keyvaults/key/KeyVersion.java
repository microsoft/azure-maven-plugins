/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.security.keyvault.keys.models.KeyProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.keyvaults.Credential;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KeyVersion extends AbstractAzResource<KeyVersion, Key, KeyProperties> implements CredentialVersion {
    public static final String SHOW_SECRET_COMMAND = "az keyvault key show --name %s --vault-name %s --version %s";
    public static final String DOWNLOAD_SECRET_COMMAND = "az keyvault key download --name %s --vault-name %s --version %s --file %s";

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

    @NotNull
    @Override
    public Credential getCredential() {
        return getParent();
    }

    @Override
    public void enable() {
        // todo: migrate to use draft
        final KeyProperties remote = getRemote();
        remote.setEnabled(true);
        doModify(() -> getKeyVault().getKeyClient().updateKeyProperties(remote).block(), AzResource.Status.UPDATING);
    }

    @Override
    public void disable() {
        // todo: migrate to use draft
        final KeyProperties remote = getRemote();
        remote.setEnabled(false);
        doModify(() -> getKeyVault().getKeyClient().updateKeyProperties(remote).block(), AzResource.Status.UPDATING);
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::isEnabled).orElse(false);
    }

    @Override
    public String getShowCredentialCommand() {
        return String.format(SHOW_SECRET_COMMAND, getCredential().getName(), getKeyVault().getName(), getName());
    }

    @Override
    public String getDownloadCredentialCommand(@Nonnull final String path) {
        return String.format(DOWNLOAD_SECRET_COMMAND, getCredential().getName(), getKeyVault().getName(), getName(), path);
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
}


