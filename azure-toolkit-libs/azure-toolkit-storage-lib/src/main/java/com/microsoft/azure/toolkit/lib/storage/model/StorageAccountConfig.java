/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class StorageAccountConfig implements IStorageAccountEntity {

    private String name;
    private String id;
    private ResourceGroup resourceGroup;
    private Subscription subscription;
    private Region region;

    private Performance performance;
    private Kind kind;
    private Redundancy redundancy;
    private AccessTier accessTier;

    @Override
    public String getSubscriptionId() {
        return Objects.nonNull(subscription) ? subscription.getId() : null;
    }

    @Override
    public String getResourceGroupName() {
        return Objects.nonNull(resourceGroup) ? resourceGroup.getName() : null;
    }
}
