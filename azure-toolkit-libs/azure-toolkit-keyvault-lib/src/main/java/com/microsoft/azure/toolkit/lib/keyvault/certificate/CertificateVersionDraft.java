/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class CertificateVersionDraft extends CertificateVersion
    implements AzResource.Draft<CertificateVersion, CertificateProperties> {

    @Getter
    private final CertificateVersion origin;

    @Setter
    private CertificateDraft.Config config;

    protected CertificateVersionDraft(@Nonnull CertificateVersion origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.create_certificate_version.version", params = {"this.getName()"})
    public CertificateProperties createResourceInAzure() {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_certificate_version.version", params = {"this.getName()"})
    public CertificateProperties updateResourceInAzure(@Nonnull CertificateProperties origin) {
        final CertificateAsyncClient secretClient = Objects.requireNonNull(getKeyVault().getCertificateClient());
        final Boolean isEnabled = ensureConfig().getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            origin.setEnabled(isEnabled);
            return Objects.requireNonNull(secretClient.updateCertificateProperties(origin).block(), "failed to update secret").getProperties();
        }
        return origin;
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private CertificateDraft.Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(CertificateDraft.Config::new);
        return this.config;
    }

    public void setEnabled(boolean b) {
        ensureConfig().setEnabled(b);
    }
}


