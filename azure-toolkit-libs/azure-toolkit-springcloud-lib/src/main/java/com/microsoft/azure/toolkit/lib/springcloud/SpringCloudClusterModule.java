/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

public class SpringCloudClusterModule extends AbstractAzResourceModule<SpringCloudCluster, SpringCloudServiceSubscription, SpringService> {

    public static final String NAME = "Spring";

    public SpringCloudClusterModule(@Nonnull SpringCloudServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    public SpringServices getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppPlatformManager::springServices).orElse(null);
    }

    @Nullable
    @Override
    public SpringCloudCluster get(@Nonnull String name, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(resourceGroup) || StringUtils.equalsIgnoreCase(resourceGroup, RESOURCE_GROUP_PLACEHOLDER)) {
            return this.list().stream().filter(c -> StringUtils.equalsIgnoreCase(name, c.getName())).findAny().orElse(null);
        }
        return super.get(name, resourceGroup);
    }

    @Nonnull
    protected SpringCloudCluster newResource(@Nonnull SpringService r) {
        return new SpringCloudCluster(r, this);
    }

    @Nonnull
    protected SpringCloudCluster newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SpringCloudCluster(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<SpringCloudCluster, SpringService> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        assert rgName != null : "'Resource group' is required.";
        return new SpringCloudClusterDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<SpringCloudCluster, SpringService> newDraftForUpdate(@Nonnull SpringCloudCluster springCloudCluster) {
        return new SpringCloudClusterDraft(springCloudCluster);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Spring Apps";
    }
}
