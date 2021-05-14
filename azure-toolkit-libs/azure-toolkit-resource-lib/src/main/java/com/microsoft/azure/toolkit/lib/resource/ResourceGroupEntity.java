/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nonnull;


@Getter
@Setter(AccessLevel.PRIVATE)
@SuperBuilder(toBuilder = true)
public class ResourceGroupEntity implements IAzureResourceEntity {

    private final String subscriptionId;
    private final String name;
    private final String id;
    private final String region;
    @Getter(AccessLevel.PACKAGE)
    private ResourceGroup inner;

    private ResourceGroupEntity(@Nonnull ResourceGroup resource) {
        this.inner = resource;
        final ResourceId resourceId = ResourceId.fromString(this.inner.id());
        this.subscriptionId = resourceId.subscriptionId();
        this.name = resource.name();
        this.region = resource.regionName();
        this.id = resource.id();
    }

    public static ResourceGroupEntity fromResource(@Nonnull ResourceGroup resource) {
        return new ResourceGroupEntity(resource);
    }

}
