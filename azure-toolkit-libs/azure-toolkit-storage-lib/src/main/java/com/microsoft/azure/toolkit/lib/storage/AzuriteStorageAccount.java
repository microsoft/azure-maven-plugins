/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.core.util.paging.ContinuablePage;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.storage.model.AccessTier;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AzuriteStorageAccount extends StorageAccount {

    public static final String AZURITE_RESOURCE_ID = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/azurite/providers/Microsoft.Storage/storageAccounts/azurite";
    public static final String AZURITE = "Azurite";
    public static final String NAME = "Storage Emulator (Azurite)";
    public static final AzuriteStorageAccount AZURITE_STORAGE_ACCOUNT = new AzuriteStorageAccount(AzuriteStorageAccountModule.AZURITE_STORAGE_ACCOUNT_MODULE);
    // Refers https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite
    public static final String AZURITE_CONNECTION_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1;"; // [SuppressMessage("Microsoft.Security", "CS001:SecretInline", Justification="public credential for azurite, refers https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite")]
    public static final String AZURITE_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="; // [SuppressMessage("Microsoft.Security", "CS001:SecretInline", Justification="public credential for azurite, refers https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite")]
    private static final String CONNECTION_NAME = "devstoreaccount1";
    // todo: @hanli support customized port for azurite
    private static final String BLOBS_URI = "http://127.0.0.1:10000/devstoreaccount1";
    private static final String QUEUES_URI = "http://127.0.0.1:10001/devstoreaccount1";
    private static final String TABLES_URI = "http://127.0.0.1:10002/devstoreaccount1";

    protected AzuriteStorageAccount(@Nonnull StorageAccountModule module) {
        super(NAME, AZURITE, module);
    }

    @Nonnull
    @Override
    public String getStatus(boolean immediately) {
        final boolean isAzuriteAccessible = canHaveBlobs() || canHaveQueues() || canHaveTables();
        return isAzuriteAccessible ? "Running" : "Stopped";
    }

    @Nonnull
    @Override
    public String loadStatus(@Nullable com.azure.resourcemanager.storage.models.StorageAccount remote) {
        return getStatus(false);
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Nonnull
    @Override
    public String getId() {
        return AZURITE_RESOURCE_ID;
    }

    @Nonnull
    @Override
    public String getConnectionString() {
        return AZURITE_CONNECTION_STRING;
    }

    @Nonnull
    @Override
    public String getKey() {
        return AZURITE_KEY;
    }

    @Nullable
    @Override
    public Region getRegion() {
        return null;
    }

    @Nullable
    @Override
    public Performance getPerformance() {
        return null;
    }

    public boolean canHaveQueues() {
        return isAzuriteEndpointAccessible(QUEUES_URI);
    }

    public boolean canHaveTables() {
        return isAzuriteEndpointAccessible(TABLES_URI);
    }

    public boolean canHaveBlobs() {
        return isAzuriteEndpointAccessible(BLOBS_URI);
    }

    public boolean canHaveShares() {
        return false;
    }

    @Nullable
    @Override
    public Redundancy getRedundancy() {
        return null;
    }

    @Nullable
    @Override
    public Kind getKind() {
        return null;
    }

    @Nullable
    @Override
    public AccessTier getAccessTier() {
        return null;
    }

    @Override
    public boolean isEmulatorResource() {
        return true;
    }

    private static boolean isAzuriteEndpointAccessible(@Nonnull final String endpoint) {
        return Utils.isUrlAccessible(endpoint, HttpStatus.SC_OK, HttpStatus.SC_ACCEPTED, HttpStatus.SC_BAD_REQUEST);
    }

    static class AzuriteStorageAccountModule extends StorageAccountModule {
        private static final AzuriteStorageAccountModule AZURITE_STORAGE_ACCOUNT_MODULE = new AzuriteStorageAccountModule(new StorageServiceSubscription("", Azure.az(AzureStorageAccount.class)));

        public AzuriteStorageAccountModule(@Nonnull StorageServiceSubscription parent) {
            super(parent);
        }

        @Nonnull
        @Override
        public List<StorageAccount> list() {
            return Arrays.asList(AZURITE_STORAGE_ACCOUNT);
        }

        @Nonnull
        @Override
        protected Iterator<? extends ContinuablePage<String, com.azure.resourcemanager.storage.models.StorageAccount>> loadResourcePagesFromAzure() {
            return Collections.emptyIterator();
        }

        @Nullable
        @Override
        protected com.azure.resourcemanager.storage.models.StorageAccount loadResourceFromAzure(@Nonnull String name, @org.jetbrains.annotations.Nullable String resourceGroup) {
            return null;
        }

        @Nonnull
        @Override
        protected StorageAccount newResource(@Nonnull com.azure.resourcemanager.storage.models.StorageAccount storageAccount) {
            throw new AzureToolkitRuntimeException("not supported");
        }

        @Nonnull
        @Override
        protected StorageAccount newResource(@Nonnull String name, @Nullable String resourceGroupName) {
            throw new AzureToolkitRuntimeException("not supported");
        }
    }
}
