/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;

@Log4j2
public class AzureResources extends AbstractAzService<ResourceGroupManager, ResourceManager> {
    public AzureResources() {
        super("Microsoft.Resources");
    }

    @Nonnull
    public ResourceGroupModule groups(@Nonnull String subscriptionId) {
        final ResourceGroupManager rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getGroupModule();
    }

    @Nonnull
    @Override
    protected ResourceManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        return AbstractAzResourceManager.getResourceManager(subscriptionId);
    }

    @Override
    protected ResourceGroupManager newResource(@Nonnull ResourceManager remote) {
        return new ResourceGroupManager(remote, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Resource groups";
    }
}
