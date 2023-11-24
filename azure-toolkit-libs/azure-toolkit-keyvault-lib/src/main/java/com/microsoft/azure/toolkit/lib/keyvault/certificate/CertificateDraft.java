/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.certificate;

import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

public class CertificateDraft extends Certificate implements AzResource.Draft<Certificate, CertificateProperties> {

    @Getter
    private Certificate origin = null;

    @Setter
    private Config config;

    protected CertificateDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull CertificateModule module) {
        super(name, resourceGroup, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.create_certificate.certificate", params = {"this.getName()"})
    public CertificateProperties createResourceInAzure() {
        return CertificateVersionDraft.createCertificateVersion(getKeyVault(), ensureConfig());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_certificate.certificate", params = {"this.getName()"})
    public CertificateProperties updateResourceInAzure(@Nonnull CertificateProperties origin) {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    private Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(CertificateDraft.Config::new);
        return this.config;
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private String name;
        private Path path;
        private String password;
        private Boolean enabled = Boolean.TRUE;
        private OffsetDateTime activationDate;
        private OffsetDateTime expirationDate;
    }
}
