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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

public class SecretDraft extends Secret implements AzResource.Draft<Secret, SecretProperties> {

    @Getter
    private Secret origin;

    @Setter
    private Config config;

    protected SecretDraft(@Nonnull String name, @Nullable String resourceGroup, @Nonnull SecretModule module) {
        super(name, Objects.requireNonNull(resourceGroup), module);
    }

    protected SecretDraft(@Nonnull Secret origin) {
        super(origin);
        this.origin = origin;
    }


    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.create_secret.secret", params = {"this.getName()"})
    public SecretProperties createResourceInAzure() {
        final SecretAsyncClient secretClient = getKeyVault().getSecretClient();
        return createOrUpdateSecret(secretClient, ensureConfig());
    }

    @Nonnull
    public static SecretProperties createOrUpdateSecret(@Nonnull final SecretAsyncClient secretClient, @Nonnull final Config config) {
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

    @Nonnull
    @Override
    @AzureOperation(name = "azure/keyvault.update_secret.secret", params = {"this.getName()"})
    public SecretProperties updateResourceInAzure(@Nonnull SecretProperties origin) {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(SecretDraft.Config::new);
        return this.config;
    }

    public void setValue(String value) {
        ensureConfig().setValue(value);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private String name;
        private String value;
        private String contentType;
        private Boolean enabled = Boolean.TRUE;
        private Boolean enableActivationDate = Boolean.FALSE;
        private OffsetDateTime activationDate;
        private Boolean enableExpirationDate = Boolean.FALSE;
        private OffsetDateTime expirationDate;
    }
}
