/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Secret extends AbstractAzResource<Secret, KeyVault, SecretProperties> implements Deletable {

    private final SecretVersionModule versionModule;

    protected Secret(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull SecretModule module) {
        super(name, resourceGroupName, module);
        this.versionModule = new SecretVersionModule(this);
    }

    protected Secret(@Nonnull Secret origin) {
        super(origin);
        this.versionModule = origin.versionModule;
    }

    protected Secret(@Nonnull SecretProperties remote, @Nonnull SecretModule module) {
        super(remote.getName(), module.getParent().getResourceGroupName(), module);
        this.versionModule = new SecretVersionModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.versionModule);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull SecretProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    @Nullable
    public SecretVersion getCurrentVersion() {
        return Optional.ofNullable(getCurrentVersionId()).map(id -> this.versionModule.get(id, getResourceGroupName())).orElse(null);
    }

    @Nullable
    public String getCurrentVersionId() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::getVersion).orElse(null);
    }

    public SecretVersionModule versions() {
        return this.versionModule;
    }

    @Nullable
    public Boolean isManaged() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::isManaged).orElse(null);
    }

    @Override
    protected void setRemote(@Nullable SecretProperties remote) {
        final String version = Optional.ofNullable(remote).map(SecretProperties::getVersion).orElse(null);
        if (StringUtils.isNotBlank(version)) {
            super.setRemote(remote);
        }
    }
}


