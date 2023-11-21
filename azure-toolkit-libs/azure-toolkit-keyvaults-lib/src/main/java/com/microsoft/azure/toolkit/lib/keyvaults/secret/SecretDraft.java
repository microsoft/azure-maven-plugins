/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
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
    public SecretProperties createResourceInAzure() {
        final SecretAsyncClient secretClient = getKeyVault().getSecretClient();
        final String value = ensureConfig().getValue();
        return Objects.requireNonNull(secretClient.setSecret(this.getName(), value).block()).getProperties();
    }

    @Nonnull
    @Override
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
        private Boolean enabled = Boolean.TRUE;
        private Boolean enableActivationDate = Boolean.FALSE;
        private OffsetDateTime activationDate;
        private Boolean enableExpirationDate = Boolean.FALSE;
        private OffsetDateTime expirationDate;
    }
}
