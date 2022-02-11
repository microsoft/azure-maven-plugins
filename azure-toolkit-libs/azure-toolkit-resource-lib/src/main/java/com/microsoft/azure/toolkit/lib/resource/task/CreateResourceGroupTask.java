/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;

/**
 * Create the resource group if the specified resource group name doesn't exist:
 * `az group create -l westus -n MyResourceGroup`
 */
public class CreateResourceGroupTask extends AzureTask<ResourceGroup> {
    private final String subscriptionId;
    private final String resourceGroupName;
    private final Region region;

    public CreateResourceGroupTask(String subscriptionId, String resourceGroupName, Region region) {
        this.subscriptionId = subscriptionId;
        this.resourceGroupName = resourceGroupName;
        this.region = region;
    }

    @Override
    @AzureOperation(name = "group.create.rg", params = {"this.resourceGroupName"}, type = AzureOperation.Type.SERVICE)
    public ResourceGroup execute() {
        return Azure.az(AzureResources.class).groups(subscriptionId)
            .createResourceGroupIfNotExist(this.resourceGroupName, this.region);
    }
}
