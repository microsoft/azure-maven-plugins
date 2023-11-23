/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import java.util.List;
import java.util.Optional;

public interface Credential extends AzResource {
    KeyVault getKeyVault();

    CredentialVersion getCurrentVersion();

    List<? extends CredentialVersion> listVersions();
    default void enable() {
        Optional.ofNullable(getCurrentVersion()).ifPresent(CredentialVersion::enable);
    }

    default void disable() {
        Optional.ofNullable(getCurrentVersion()).ifPresent(CredentialVersion::enable);
    }

    default Boolean isEnabled() {
        return Optional.ofNullable(getCurrentVersion()).map(CredentialVersion::isEnabled).orElse(false);
    }
}
