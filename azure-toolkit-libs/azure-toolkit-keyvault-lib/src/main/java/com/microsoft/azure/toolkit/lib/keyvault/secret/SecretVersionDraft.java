/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.secret;

import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyConfiureAction;
import static com.microsoft.azure.toolkit.lib.keyvault.KeyVault.getAccessPolicyLearnMoreAction;

public class SecretVersionDraft extends SecretVersion
    implements AzResource.Draft<SecretVersion, SecretProperties> {
    public static final String SECRET_CREATION_FORBIDDEN_MESSAGE = "failed to create secret %s, access denied, please make sure that you have access policy defined to do this operation";
    public static final String SECRET_CREATION_FAILED_MESSAGE = "failed to create secret %s, an unexpected error occurred";
    public static final String SECRET_UPDATE_FORBIDDEN_MESSAGE = "failed to create secret %s, access denied, please make sure that you have access policy defined to do this operation";
    public static final String SECRET_UPDATE_FAILED_MESSAGE = "failed to create secret %s, an unexpected error occurred";

    @Getter
    private final SecretVersion origin;

    @Setter
    private SecretDraft.Config config;

    protected SecretVersionDraft(@Nonnull SecretVersion origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.create_secret_version.version", params = {"this.getName()"})
    public SecretProperties createResourceInAzure() {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_secret_version.version", params = {"this.getName()"})
    public SecretProperties updateResourceInAzure(@Nonnull SecretProperties origin) {
        final SecretDraft.Config config = ensureConfig();
        final Boolean isEnabled = config.getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            return updateSecretVersion(getKeyVault(), origin, config);
        }
        return origin;
    }

    @Nonnull
    public static SecretProperties createSecretVersion(@Nonnull final KeyVault keyVault, @Nonnull final SecretDraft.Config config) {
        final SecretAsyncClient secretClient = keyVault.getSecretClient();
        try {
            final String value = config.getValue();
            final KeyVaultSecret secret = secretClient.setSecret(config.getName(), value).block();
            final SecretProperties properties = Objects.requireNonNull(secret).getProperties();
            Optional.ofNullable(config.getEnabled()).ifPresent(properties::setEnabled);
            Optional.ofNullable(config.getContentType()).ifPresent(properties::setContentType);
            return Objects.requireNonNull(secretClient.updateSecretProperties(properties).block());
        } catch (final Throwable t) {
            // swallow all exceptions to prevent credential leakage
            final Action<String> configure = getAccessPolicyConfiureAction(keyVault);
            final Action<String> learnMore = getAccessPolicyLearnMoreAction();
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(SECRET_CREATION_FORBIDDEN_MESSAGE, config.getName()), configure, learnMore);
            }
            throw new AzureToolkitRuntimeException(String.format(SECRET_CREATION_FAILED_MESSAGE, config.getName()));
        }
    }

    public static SecretProperties updateSecretVersion(@Nonnull final KeyVault keyVault,
                                                       @Nonnull final SecretProperties origin,
                                                       @Nonnull final SecretDraft.Config config) {
        final SecretAsyncClient client = keyVault.getSecretClient();
        try {
            origin.setEnabled(config.getEnabled());
            return Objects.requireNonNull(client.updateSecretProperties(origin).block(), "failed to update secret");
        } catch (final Throwable t) {
            // swallow all exceptions to prevent credential leakage
            final Action<String> configure = getAccessPolicyConfiureAction(keyVault);
            final Action<String> learnMore = getAccessPolicyLearnMoreAction();
            if (isHttpException(t, 403)) {
                throw new AzureToolkitRuntimeException(String.format(SECRET_UPDATE_FORBIDDEN_MESSAGE, config.getName()), configure, learnMore);
            }
            throw new AzureToolkitRuntimeException(String.format(SECRET_UPDATE_FAILED_MESSAGE, config.getName()));
        }
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private SecretDraft.Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(SecretDraft.Config::new);
        return this.config;
    }

    public void setEnabled(boolean b) {
        ensureConfig().setEnabled(b);
    }
}

