package com.microsoft.azure.toolkit.lib.cosmos;

import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;

import java.util.List;

public interface ICosmosDatabase extends AzResourceBase {
    List<? extends ICosmosCollection> listCollection();
}
