/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.ProviderResourceType;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public interface IResourceManager {

    default List<Region> listSupportedRegions(String resourceType) {
        final String provider = getService().getName();
        final String subscriptionId = this.getSubscriptionId();
        List<Region> allRegionList = az(IAzureAccount.class).listRegions(subscriptionId);
        List<Region> result = new ArrayList<>();
        final ResourceManager resourceManager = getResourceManager();
        resourceManager.providers().getByName(provider).resourceTypes()
            .stream().filter(type -> StringUtils.equalsIgnoreCase(type.resourceType(), resourceType))
            .findAny().map(ProviderResourceType::locations)
            .ifPresent(list -> result.addAll(list.stream().map(Region::fromName).filter(allRegionList::contains).collect(Collectors.toList())));
        return result.isEmpty() ? allRegionList : result;
    }

    String getSubscriptionId();

    AzService getService();

    ResourceManager getResourceManager();
}
