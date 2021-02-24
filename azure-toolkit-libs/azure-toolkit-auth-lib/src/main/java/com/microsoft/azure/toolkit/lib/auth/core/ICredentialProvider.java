/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core;

import com.azure.core.credential.TokenCredential;

public interface  ICredentialProvider {
    TokenCredential provideCredentialForTenant(String tenantId);

    TokenCredential provideCredentialCommon();
}
