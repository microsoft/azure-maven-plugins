/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.secret;

import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.keyvault.Credential;
import com.microsoft.azure.toolkit.lib.keyvault.CredentialVersion;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.CertificateVersionDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Secret extends AbstractAzResource<Secret, KeyVault, SecretProperties> implements Deletable, Credential {

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

    @Override
    public KeyVault getKeyVault() {
        return getParent();
    }

    @Nullable
    public SecretVersion getCurrentVersion() {
        return Optional.ofNullable(getCurrentVersionId()).map(id -> this.versionModule.get(id, getResourceGroupName())).orElse(null);
    }

    @Override
    public List<? extends CredentialVersion> listVersions() {
        return versions().list();
    }

    @Nullable
    public String getCurrentVersionId() {
        return this.versions().list().stream()
            .filter(version -> Objects.nonNull(version.getProperties()))
            .max(Comparator.comparing(version -> version.getProperties().getCreatedOn()))
            .map(SecretVersion::getVersion).orElse(null);
    }

    public SecretVersionModule versions() {
        return this.versionModule;
    }

    @Nullable
    public Boolean isManaged() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::isManaged).orElse(null);
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(SecretProperties::isEnabled).orElse(false);
    }

    @Nullable
    public SecretProperties getProperties() {
        return getRemote();
    }

    // as version id for new secret version is generated by Azure, which is not compatiable with our existing model
    // change to create new one with sdk and get it from Azure
    @Nullable
    public SecretVersion addNewSecretVersion(final String value) {
        final SecretAsyncClient secretClient = getKeyVault().getSecretClient();
        if (Objects.isNull(secretClient)) {
            return null;
        }
        final KeyVaultSecret block = secretClient.setSecret(getName(), value).block();
        this.versionModule.refresh();
        return Optional.ofNullable(block).map(secret -> this.versionModule.get(block.getProperties().getVersion(), getResourceGroupName())).orElse(null);
    }

    @Nullable
    public SecretVersion addNewSecretVersion(final SecretDraft.Config value) {
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating new Secret Version for Secret ({0}).", this.getName()));
        final SecretProperties secret = SecretVersionDraft.createSecretVersion(getKeyVault(), value);
        messager.info(AzureString.format("New Secret Version ({0}) is successfully created for Secret ({1}).", secret.getVersion(), this.getName()));
        this.refresh();
        return this.versionModule.get(secret.getVersion(), getResourceGroupName());
    }
}


