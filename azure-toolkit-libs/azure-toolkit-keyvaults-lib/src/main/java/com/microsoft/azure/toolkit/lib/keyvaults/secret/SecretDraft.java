/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults.secret;

import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class SecretDraft extends Secret implements AzResource.Draft<Secret, SecretProperties> {

    @Getter
    private Secret origin;

    @Setter
    private String value;

    protected SecretDraft(@Nonnull String name, @Nullable String resourceGroup, @Nonnull SecretModule module) {
        super(name, resourceGroup, module);
    }

    protected SecretDraft(@Nonnull Secret origin) {
        super(origin);
        this.origin = origin;
    }


    @Override
    public void reset() {
        this.value = null;
    }

    @Nonnull
    @Override
    public SecretProperties createResourceInAzure() {
        final SecretAsyncClient secretClient = getKeyVault().getSecretClient();
        return secretClient.setSecret(this.getName(), this.value).block().getProperties();
    }

    @Nonnull
    @Override
    public SecretProperties updateResourceInAzure(@Nonnull SecretProperties origin) {
        throw new AzureToolkitRuntimeException("Not support update secret");
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(value);
    }
}
