/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.ImportCertificateOptions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
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
    public CertificateProperties createResourceInAzure() {
        final CertificateAsyncClient secretClient = getKeyVault().getCertificateClient();
        return createOrUpdateCertificate(secretClient, ensureConfig());
    }

    @Nonnull
    @Override
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

    @Nonnull
    public static CertificateProperties createOrUpdateCertificate(@Nonnull final CertificateAsyncClient secretClient,
                                                                  @Nonnull Config config) {
        final Path path = Objects.requireNonNull(config.getPath(), "'path' is required");
        final String password = config.getPassword();
        final Boolean isEnabled = config.getEnabled();
        try (final FileInputStream inputStream = new FileInputStream(path.toFile())) {
            final byte[] byteArray = IOUtils.toByteArray(inputStream);
            final ImportCertificateOptions options = new ImportCertificateOptions(config.getName(), byteArray);
            options.setEnabled(BooleanUtils.isNotFalse(isEnabled));
            Optional.ofNullable(password).filter(StringUtils::isNoneEmpty).ifPresent(options::setPassword);
            return Objects.requireNonNull(secretClient.importCertificate(options).block(), "failed to import certificate").getProperties();
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(e);
        }
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
