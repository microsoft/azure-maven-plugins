/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.ImportCertificateOptions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class CertificateVersionDraft extends CertificateVersion
    implements AzResource.Draft<CertificateVersion, CertificateProperties> {

    public static final String CERTIFICATE_CREATION_FORBIDDEN_MESSAGE = "failed to create certificate %s, access denied";
    public static final String CERTIFICATE_CREATION_FAILED_MESSAGE = "failed to create certificate %s, an unexpected error occurred";
    public static final String CERTIFICATE_UPDATE_FORBIDDEN_MESSAGE = "failed to update certificate %s, access denied";
    public static final String CERTIFICATE_UPDATE_FAILED_MESSAGE = "failed to update certificate %s, an unexpected error occurred";
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
        final CertificateDraft.Config config = ensureConfig();
        final Boolean isEnabled = config.getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            return updateCertificateVersion(secretClient, origin, config);
        }
        return origin;
    }

    @Nonnull
    public static CertificateProperties createCertificateVersion(@Nonnull final CertificateAsyncClient secretClient,
                                                                 @Nonnull final CertificateDraft.Config config) {
        final Path path = Objects.requireNonNull(config.getPath(), "'path' is required");
        try (final FileInputStream inputStream = new FileInputStream(path.toFile())) {
            final String password = config.getPassword();
            final Boolean isEnabled = config.getEnabled();
            final byte[] byteArray = IOUtils.toByteArray(inputStream);
            final ImportCertificateOptions options = new ImportCertificateOptions(config.getName(), byteArray);
            options.setEnabled(BooleanUtils.isNotFalse(isEnabled));
            Optional.ofNullable(password).filter(StringUtils::isNoneEmpty).ifPresent(options::setPassword);
            return Objects.requireNonNull(secretClient.importCertificate(options).block(), "failed to import certificate").getProperties();
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(e);
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(CERTIFICATE_CREATION_FORBIDDEN_MESSAGE, config.getName()));
            }
            throw new AzureToolkitRuntimeException(String.format(CERTIFICATE_CREATION_FAILED_MESSAGE, config.getName()));
        }
    }

    public static CertificateProperties updateCertificateVersion(@Nonnull final CertificateAsyncClient client,
                                                                 @Nonnull CertificateProperties origin,
                                                                 @Nonnull final CertificateDraft.Config config) {
        try {
            origin.setEnabled(config.getEnabled());
            return Objects.requireNonNull(client.updateCertificateProperties(origin).block(), "failed to update secret").getProperties();
        } catch (final Throwable t) {
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(CERTIFICATE_UPDATE_FORBIDDEN_MESSAGE, config.getName()));
            }
            throw new AzureToolkitRuntimeException(String.format(CERTIFICATE_UPDATE_FAILED_MESSAGE, config.getName()));
        }
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


