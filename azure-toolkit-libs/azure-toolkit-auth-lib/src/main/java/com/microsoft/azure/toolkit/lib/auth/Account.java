/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;

public class Account {

    public Account login(AccountEntity account) {
        return this;
    }

    public AzureCredentialWrapper getCredentialWrapper(String tenantId) {
        return null;
    }
}
