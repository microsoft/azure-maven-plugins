/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConnectionStringStorageAccountModule extends AbstractConnectionStringAzResourceModule<ConnectionStringStorageAccount> {
    private static final String RESOURCE_ID = String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/", CONNECTION_STRING_SUBSCRIPTION_ID, CONNECTION_STRING_RESOURCE_GROUP);
    private static ConnectionStringStorageAccountModule instance = null;

    protected ConnectionStringStorageAccountModule() {
        super("storageAccounts", AzResource.NONE);
    }

    public synchronized static ConnectionStringStorageAccountModule getInstance() {
        if (instance == null) {
            instance = new ConnectionStringStorageAccountModule();
        }
        return instance;
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull final String resourceName, @Nullable final String resourceGroup) {
        return RESOURCE_ID + resourceName;
    }

    @Nonnull
    @Override
    protected ConnectionStringStorageAccount newResource(@Nonnull final String s) {
        return new ConnectionStringStorageAccount(s);
    }

    @Override
    public String getServiceNameForTelemetry() {
        return "storage";
    }
}
