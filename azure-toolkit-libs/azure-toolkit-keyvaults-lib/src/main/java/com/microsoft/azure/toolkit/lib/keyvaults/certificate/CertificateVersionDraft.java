/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class CertificateVersionDraft extends CertificateVersion
    implements AzResource.Draft<CertificateVersion, CertificateProperties> {

    @Getter
    private final CertificateVersion origin;

    @Setter
    private Config config;

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
    public CertificateProperties createResourceInAzure() {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Nonnull
    @Override
    public CertificateProperties updateResourceInAzure(@Nonnull CertificateProperties origin) {
        final CertificateAsyncClient secretClient = Objects.requireNonNull(getKeyVault().getCertificateClient());
        final Boolean isEnabled = ensureConfig().getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            origin.setEnabled(isEnabled);
            return Objects.requireNonNull(secretClient.updateCertificateProperties(origin).block().getProperties(), "failed to update secret");
        }
        return origin;
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private Config ensureConfig() {
        return Optional.ofNullable(config).orElseGet(Config::new);
    }

    public void setEnabled(boolean b) {
        ensureConfig().setEnabled(b);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Config {
        private Boolean enabled;
    }
}


