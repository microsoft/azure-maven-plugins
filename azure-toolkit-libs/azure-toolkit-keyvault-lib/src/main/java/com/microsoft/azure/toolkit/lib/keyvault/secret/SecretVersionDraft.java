/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault.secret;

import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class SecretVersionDraft extends SecretVersion
    implements AzResource.Draft<SecretVersion, SecretProperties> {

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
        final SecretAsyncClient secretClient = Objects.requireNonNull(getKeyVault().getSecretClient());
        final SecretDraft.Config config = ensureConfig();
        final Boolean isEnabled = config.getEnabled();
        final boolean isModified = Objects.nonNull(isEnabled) && !Objects.equals(isEnabled, origin.isEnabled());
        if (isModified) {
            return updateSecretVersion(secretClient, origin, config);
        }
        return origin;
    }

    @Nonnull
    public static SecretProperties createSecretVersion(@Nonnull final SecretAsyncClient secretClient, @Nonnull final SecretDraft.Config config) {
        try {
            final String value = config.getValue();
            final KeyVaultSecret secret = secretClient.setSecret(config.getName(), value).block();
            final SecretProperties properties = Objects.requireNonNull(secret).getProperties();
            Optional.ofNullable(config.getEnabled()).ifPresent(properties::setEnabled);
            Optional.ofNullable(config.getContentType()).ifPresent(properties::setContentType);
            return Objects.requireNonNull(secretClient.updateSecretProperties(properties).block());
        } catch (final Throwable e) {
            throw new AzureToolkitRuntimeException("failed to create secret, please check whether you have correct permission and try again");
        }
    }

    public static SecretProperties updateSecretVersion(@Nonnull final SecretAsyncClient client,
                                                       @Nonnull SecretProperties origin,
                                                       @Nonnull final SecretDraft.Config config) {
        try {
            origin.setEnabled(config.getEnabled());
            return Objects.requireNonNull(client.updateSecretProperties(origin).block(), "failed to update secret");
        } catch (final Throwable t) {
            throw new AzureToolkitRuntimeException("failed to update key, please check whether you have correct permission and try again");
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

