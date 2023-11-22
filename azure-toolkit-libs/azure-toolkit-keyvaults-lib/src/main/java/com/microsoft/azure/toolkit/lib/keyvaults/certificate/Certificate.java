/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.keyvaults.Credential;
import com.microsoft.azure.toolkit.lib.keyvaults.CredentialVersion;
import com.microsoft.azure.toolkit.lib.keyvaults.KeyVault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Certificate extends AbstractAzResource<Certificate, KeyVault, CertificateProperties> implements Deletable, Credential {
    private final CertificateVersionModule versionModule;

    protected Certificate(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CertificateModule module) {
        super(name, resourceGroupName, module);
        this.versionModule = new CertificateVersionModule(this);
    }

    protected Certificate(@Nonnull Certificate origin) {
        super(origin);
        this.versionModule = new CertificateVersionModule(this);
    }

    protected Certificate(@Nonnull CertificateProperties remote, @Nonnull CertificateModule module) {
        super(remote.getName(), module.getParent().getResourceGroupName(), module);
        this.versionModule = new CertificateVersionModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.versionModule);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull CertificateProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    public CertificateVersionModule versions() {
        return this.versionModule;
    }

    @Override
    public KeyVault getKeyVault() {
        return getParent();
    }

    @Nullable
    public CertificateVersion getCurrentVersion() {
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
            .map(CertificateVersion::getVersion).orElse(null);
    }

    @Nullable
    public CertificatePolicy getPolicy() {
        final CertificateAsyncClient client = getParent().getCertificateClient();
        if (Objects.isNull(client)) {
            return null;
        }
        return client.getCertificatePolicy(getName()).block();
    }


    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(CertificateProperties::isEnabled).orElse(false);
    }

    @Nullable
    public CertificateProperties getProperties() {
        return getRemote();
    }

    @Nullable
    public CertificateVersion addNewCertificateVersion(final CertificateDraft.Config config) {
        final CertificateAsyncClient client = getKeyVault().getCertificateClient();
        final CertificateProperties certificate = CertificateDraft.createOrUpdateCertificate(client, config);
        this.refresh();
        return Optional.of(certificate).map(secret -> this.versionModule.get(certificate.getVersion(), getResourceGroupName())).orElse(null);
    }
}

