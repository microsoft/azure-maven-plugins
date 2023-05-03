package com.microsoft.azure.toolkit.lib.cosmos;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import java.util.List;

public interface ICosmosDatabase extends AzResource {
    List<? extends ICosmosCollection> listCollection();
}
