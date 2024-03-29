/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.certificate;

import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.ImportCertificateOptions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyConfiureAction;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyLearnMoreAction;

public class CertificateVersionDraft extends CertificateVersion
    implements AzResource.Draft<CertificateVersion, CertificateProperties> {

    public static final String CERTIFICATE_CREATION_FORBIDDEN_MESSAGE = "access denied, please make sure that you have access policy defined to do this operation";
    public static final String CERTIFICATE_CREATION_FAILED_MESSAGE = "an unexpected error occurred";
    public static final String CERTIFICATE_UPDATE_FORBIDDEN_MESSAGE = "access denied, please make sure that you have access policy defined to do this operation";
    public static final String CERTIFICATE_UPDATE_FAILED_MESSAGE = "an unexpected error occurred";
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
        final CertificateDraft.Config config = ensureConfig();
        final Boolean isEnabled = config.getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Certificate Version ({0}).", this.getVersion()));
            final CertificateProperties properties = updateCertificateVersion(getKeyVault(), origin, config);
            messager.info(AzureString.format("Certificate Version ({0}) is successfully updated.", this.getVersion()));
            return properties;
        }
        return origin;
    }

    @Nonnull
    public static CertificateProperties createCertificateVersion(@Nonnull final KeyVault keyVault,
                                                                 @Nonnull final CertificateDraft.Config config) {
        final CertificateAsyncClient secretClient = keyVault.getCertificateClient();
        final Path path = Objects.requireNonNull(config.getPath(), "'path' is required");
        try (final FileInputStream inputStream = new FileInputStream(path.toFile())) {
            final String password = config.getPassword();
            final Boolean isEnabled = config.getEnabled();
            final byte[] byteArray = IOUtils.toByteArray(inputStream);
            final ImportCertificateOptions options = new ImportCertificateOptions(config.getName(), byteArray);
            options.setEnabled(BooleanUtils.isNotFalse(isEnabled));
            Optional.ofNullable(password).filter(StringUtils::isNoneEmpty).ifPresent(options::setPassword);
            return Objects.requireNonNull(secretClient.importCertificate(options).block(), "failed to import certificate").getProperties();
        } catch (final Throwable t) {
            // swallow all exceptions to prevent credential leakage
            final Action<String> configure = getAccessPolicyConfiureAction(keyVault);
            final Action<String> learnMore = getAccessPolicyLearnMoreAction();
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(CERTIFICATE_CREATION_FORBIDDEN_MESSAGE, configure, learnMore);
            }
            throw new AzureToolkitRuntimeException(CERTIFICATE_CREATION_FAILED_MESSAGE);
        }
    }

    public static CertificateProperties updateCertificateVersion(@Nonnull final KeyVault keyVault,
                                                                 @Nonnull final CertificateProperties origin,
                                                                 @Nonnull final CertificateDraft.Config config) {
        final CertificateAsyncClient client = keyVault.getCertificateClient();
        try {
            origin.setEnabled(config.getEnabled());
            return Objects.requireNonNull(client.updateCertificateProperties(origin).block(), "failed to update secret").getProperties();
        } catch (final Throwable t) {
            // swallow all exceptions to prevent credential leakage
            if (isHttpException(t, 403)) {
                final Action<String> configure = getAccessPolicyConfiureAction(keyVault);
                final Action<String> learnMore = getAccessPolicyLearnMoreAction();
                throw new AzureToolkitRuntimeException(CERTIFICATE_UPDATE_FORBIDDEN_MESSAGE, configure, learnMore);
            }
            throw new AzureToolkitRuntimeException(CERTIFICATE_UPDATE_FAILED_MESSAGE);
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


