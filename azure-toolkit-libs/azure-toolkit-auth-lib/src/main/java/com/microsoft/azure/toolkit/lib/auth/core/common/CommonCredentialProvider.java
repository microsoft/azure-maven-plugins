/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.common;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialProvider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CommonCredentialProvider implements ICredentialProvider {
    private TokenCredential sharedTokenCredential;

    @Override
    public TokenCredential provideCredentialForTenant(String tenantId) {
        return sharedTokenCredential;
    }

    @Override
    public TokenCredential provideCredentialCommon() {
        return sharedTokenCredential;
    }

}
