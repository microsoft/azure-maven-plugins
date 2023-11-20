/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.key;

import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.keyvaults.Credential;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Key extends AbstractAzResource<Key, KeyVault, KeyProperties> implements Deletable, Credential {
    private final KeyVersionModule versionModule;

    protected Key(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KeyModule module) {
        super(name, resourceGroupName, module);
        this.versionModule = new KeyVersionModule(this);
    }

    protected Key(@Nonnull Key origin) {
        super(origin);
        this.versionModule = new KeyVersionModule(this);
    }

    protected Key(@Nonnull KeyProperties remote, @Nonnull KeyModule module) {
        super(remote.getName(), module.getParent().getResourceGroupName(), module);
        this.versionModule = new KeyVersionModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.versionModule);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull KeyProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    public KeyVersionModule versions() {
        return this.versionModule;
    }

    @Override
    public KeyVault getKeyVault() {
        return getParent();
    }

    @Nullable
    public KeyVersion getCurrentVersion() {
        return Optional.ofNullable(getCurrentVersionId()).map(id -> this.versionModule.get(id, getResourceGroupName())).orElse(null);
    }

    @Override
    public List<? extends CredentialVersion> listVersions() {
        return versions().list();
    }

    @Nullable
    public String getCurrentVersionId() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::getVersion).orElse(null);
    }

    @Override
    protected void setRemote(@Nullable KeyProperties remote) {
        final String version = Optional.ofNullable(remote).map(KeyProperties::getVersion).orElse(null);
        if (StringUtils.isNotBlank(version)) {
            super.setRemote(remote);
        }
    }

    @Nullable
    public Boolean isManaged() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::isManaged).orElse(null);
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(KeyProperties::isEnabled).orElse(false);
    }

    @Nullable
    public KeyProperties getProperties(){
        return getRemote();
    }
}

