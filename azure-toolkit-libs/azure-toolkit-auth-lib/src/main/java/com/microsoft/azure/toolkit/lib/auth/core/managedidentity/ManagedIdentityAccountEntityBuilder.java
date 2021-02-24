/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.managedidentity;


import com.azure.core.management.AzureEnvironment;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.util.AccountBuilderUtils;
import com.microsoft.azure.toolkit.lib.auth.core.common.DefaultCredentialProvider;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ManagedIdentityAccountEntityBuilder implements IAccountEntityBuilder {
    private AzureEnvironment environment;

    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = AccountBuilderUtils.createAccountEntity(AuthMethod.MANAGED_IDENTITY);
        accountEntity.setEnvironment(environment);

        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder().build();
        accountEntity.setCredentialBuilder(new DefaultCredentialProvider(managedIdentityCredential));
        AccountBuilderUtils.listTenants(accountEntity);
        return accountEntity;
    }

}
