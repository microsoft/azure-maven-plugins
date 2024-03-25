/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage;

import com.azure.core.http.policy.FixedDelayOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.util.logging.ClientLogger;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.implementation.connectionstring.StorageConnectionString;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResource;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerModule;
import com.microsoft.azure.toolkit.lib.storage.queue.QueueModule;
import com.microsoft.azure.toolkit.lib.storage.share.ShareModule;
import com.microsoft.azure.toolkit.lib.storage.table.TableModule;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConnectionStringStorageAccount extends AbstractConnectionStringAzResource<ConnectionStringStorageAccount> implements IStorageAccount {
    private static final ClientLogger LOGGER = new ClientLogger(ConnectionStringStorageAccount.class);
    private static final RetryOptions TEST_CONNECTION_RETRY_OPTIONS = new RetryOptions(new FixedDelayOptions(0, Duration.ofSeconds(3))); // Duration.ZERO is not supported in RequestRetryOptions

    @Getter
    protected final BlobContainerModule blobContainerModule;
    @Getter
    protected final ShareModule shareModule;
    @Getter
    protected final QueueModule queueModule;
    @Getter
    protected final TableModule tableModule;
    protected final List<AbstractAzResourceModule<?, ?, ?>> subModules = new ArrayList<>();
    private final StorageConnectionString settings;
    private boolean loaded = false;

    protected ConnectionStringStorageAccount(@Nonnull final String connectionString) {
        super(connectionString, extractNameFromConnectionString(connectionString), ConnectionStringStorageAccountModule.getInstance());
        this.settings = StorageConnectionString.create(connectionString, LOGGER);
        this.blobContainerModule = new BlobContainerModule(this);
        this.shareModule = new ShareModule(this);
        this.queueModule = new QueueModule(this);
        this.tableModule = new TableModule(this);
    }

    @Override
    public void invalidateCache() {
        this.subModules.clear();
        super.invalidateCache();
    }

    @Override
    public String getKey() {
        return this.settings.getStorageAuthSettings().getAccount().getAccessKey();
    }

    @Override
    protected synchronized void updateAdditionalProperties(@Nullable String newRemote, @Nullable String oldRemote) {
        this.subModules.clear();
        if (Objects.nonNull(newRemote)) {
            this.getSubModules();
        }
        super.updateAdditionalProperties(newRemote, oldRemote);
    }

    @Override
    public boolean exists() {
        return !this.loaded || !this.subModules.isEmpty();
    }

    @Nonnull
    public String getStatus() {
        return this.loadStatus(this.getConnectionString());
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull final String connectionString) {
        return CollectionUtils.isNotEmpty(this.subModules) ? "Running" : this.loaded ? "Failed" : "Connecting";
    }

    @Nonnull
    @Override
    public synchronized List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        this.setStatus("Connecting");
        if (!this.subModules.isEmpty()) {
            return this.subModules;
        }
        if (this.canHaveBlobs()) {
            this.subModules.add(this.blobContainerModule);
        }
        if (this.canHaveShares()) {
            this.subModules.add(this.shareModule);
        }
        if (this.canHaveQueues()) {
            this.subModules.add(this.queueModule);
        }
        if (this.canHaveTables()) {
            this.subModules.add(this.tableModule);
        }
        this.loaded = true;
        this.setStatus(CollectionUtils.isNotEmpty(this.subModules) ? "Running" : "Failed");
        return this.subModules;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getCachedSubModules() {
        return this.subModules;
    }

    public boolean canHaveQueues() {
        try {
            final QueueServiceClient client = new QueueServiceClientBuilder().retryOptions(TEST_CONNECTION_RETRY_OPTIONS).connectionString(this.getConnectionString()).buildClient();
            client.getProperties();
            return true;
        } catch (final Exception e) {
            // swallow exception when test connection
            return false;
        }
    }

    public boolean canHaveTables() {
        try {
            final TableServiceClient client = new TableServiceClientBuilder().retryOptions(TEST_CONNECTION_RETRY_OPTIONS).connectionString(this.getConnectionString()).buildClient();
            client.getProperties();
            return true;
        } catch (final Exception e) {
            // swallow exception when test connection
            return false;
        }
    }

    public boolean canHaveBlobs() {
        try {
            final BlobServiceClient client = new BlobServiceClientBuilder().retryOptions(TEST_CONNECTION_RETRY_OPTIONS).connectionString(this.getConnectionString()).buildClient();
            client.getProperties();
            return true;
        } catch (final Exception e) {
            // swallow exception when test connection
            return false;
        }
    }

    public boolean canHaveShares() {
        try {
            final ShareServiceClient client = new ShareServiceClientBuilder().retryOptions(TEST_CONNECTION_RETRY_OPTIONS).connectionString(this.getConnectionString()).buildClient();
            client.getProperties();
            return true;
        } catch (final Exception e) {
            // swallow exception when test connection
            return false;
        }
    }

    /**
     * extract storage account name from connection string.
     *
     * @param connectionString connection string
     * @return Storage account name for a given valid connection string and null otherwise.
     */
    public static String extractNameFromConnectionString(String connectionString) {
        final StorageConnectionString storageConnectionString = StorageConnectionString.create(connectionString, LOGGER);
        return storageConnectionString.getAccountName();
    }
}
