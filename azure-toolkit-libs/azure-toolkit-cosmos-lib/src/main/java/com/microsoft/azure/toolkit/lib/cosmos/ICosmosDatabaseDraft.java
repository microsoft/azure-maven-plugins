package com.microsoft.azure.toolkit.lib.cosmos;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;

public interface ICosmosDatabaseDraft<T extends AzResource<T, ?, R>, R> extends AzResource.Draft<T, R> {
    void setConfig(DatabaseConfig config);
}
