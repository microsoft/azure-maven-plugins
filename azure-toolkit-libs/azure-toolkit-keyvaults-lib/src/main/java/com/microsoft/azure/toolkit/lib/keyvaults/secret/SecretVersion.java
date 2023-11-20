/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.keyvaults.Credential;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SecretVersion extends AbstractAzResource<SecretVersion, Secret, SecretProperties> implements CredentialVersion {

    public static final String SHOW_SECRET_COMMAND = "az keyvault secret show --name %s --vault-name %s --version %s";
    public static final String DOWNLOAD_SECRET_COMMAND = "az keyvault secret download --name %s --vault-name %s --version %s --file %s";

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

    @NotNull
    @Override
    public Credential getCredential() {
        return getParent();
    }

    @Override
    public void enable() {
        final SecretVersionDraft update = (SecretVersionDraft) this.update();
        update.setEnabled(true);
        update.commit();
    }

    @Override
    public void disable() {
        final SecretVersionDraft update = (SecretVersionDraft) this.update();
        update.setEnabled(false);
        update.commit();
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::isEnabled).orElse(false);
    }

    @Override
    public String getShowCredentialCommand() {
        return String.format(SHOW_SECRET_COMMAND, getCredential().getName(), getKeyVault().getName(), getName());
    }

    @Override
    public String getDownloadCredentialCommand(@Nonnull final String path) {
        return String.format(DOWNLOAD_SECRET_COMMAND, getCredential().getName(), getKeyVault().getName(), getName(), path);
    }

    @Nullable
    public String getVersion() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::getVersion).orElse(null);
    }

    public boolean isCurrentVersion() {
        return StringUtils.equals(getVersion(), getParent().getCurrentVersionId());
    }

    @Nullable
    public SecretProperties getProperties() {
        return getRemote();
    }
}
