/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificate;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificateWithPolicy;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CertificateVersion extends AbstractAzResource<CertificateVersion, Certificate, CertificateProperties> {
    private KeyVaultCertificateWithPolicy keyVaultCertificateWithPolicy;

    protected CertificateVersion(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CertificateVersionModule module) {
        super(name, resourceGroupName, module);
    }

    protected CertificateVersion(@Nonnull CertificateVersion origin) {
        super(origin);
    }

    protected CertificateVersion(@Nonnull CertificateProperties remote, @Nonnull CertificateVersionModule module) {
        super(remote.getVersion(), module.getParent().getResourceGroupName(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull CertificateProperties remote) {
        return remote.isEnabled() ? FormalStatus.RUNNING.name() : FormalStatus.STOPPED.name();
    }

    public Boolean isEnabled() {
        return Optional.ofNullable(getRemote()).map(CertificateProperties::isEnabled).orElse(false);
    }

    public String getVersion() {
        return Optional.ofNullable(getRemote()).map(CertificateProperties::getVersion).orElse(null);
    }

    public boolean isCurrentVersion() {
        return StringUtils.equals(getVersion(), getParent().getCurrentVersionId());
    }

    @Nullable
    public byte[] getCertificateCer() {
        return Optional.ofNullable(getCertificate()).map(KeyVaultCertificate::getCer).orElse(null);
    }

    public byte[] getCertificatePem() {
        final byte[] cer = getCertificateCer();
        return Optional.ofNullable(cer).map(c -> Base64.getEncoder().encode(c)).orElse(null);
    }

    @Nullable
    public KeyVaultCertificate getCertificate() {
        final CertificateAsyncClient client = getParent().getParent().getCertificateClient();
        if (Objects.isNull(client)) {
            return null;
        }
        return client.getCertificateVersion(getParent().getName(), getVersion()).block();
    }

    @Nullable
    public CertificatePolicy getPolicy() {
        final CertificateAsyncClient client = getParent().getParent().getCertificateClient();
        if (Objects.isNull(client)) {
            return null;
        }
        return client.getCertificatePolicy(getParent().getName()).block();
    }

    @Nullable
    public CertificateProperties getProperties() {
        return getRemote();
    }
}
