package com.microsoft.azure.toolkit.lib.storage;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;

public interface IStorageAccount extends AzResource {
    @Nonnull
    String getConnectionString();
}
