/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;

public interface CredentialVersion extends AzResource {
    @Nonnull
    Credential getCredential();

    default KeyVault getKeyVault() {
        return getCredential().getKeyVault();
    }

    void enable();

    void disable();

    Boolean isEnabled();

    String getShowCredentialCommand();

    String getDownloadCredentialCommand(final String path);
}
