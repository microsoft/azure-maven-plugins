package com.microsoft.azure.toolkit.lib.storage;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerModule;
import com.microsoft.azure.toolkit.lib.storage.queue.QueueModule;
import com.microsoft.azure.toolkit.lib.storage.share.ShareModule;
import com.microsoft.azure.toolkit.lib.storage.table.TableModule;

import javax.annotation.Nonnull;

public interface IStorageAccount extends AzResource {
    @Nonnull
    String getConnectionString();

    String getKey();

    BlobContainerModule getBlobContainerModule();

    ShareModule getShareModule();

    TableModule getTableModule();

    QueueModule getQueueModule();
}
