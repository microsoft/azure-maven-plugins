/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceGroupConfig {
    private String subscriptionId;
    private String name;
    private Region region;

    @Contract("null->null")
    public static ResourceGroupConfig fromResource(@Nullable ResourceGroup group) {
        if (Objects.isNull(group)) {
            return null;
        }
        return ResourceGroupConfig.builder()
            .subscriptionId(group.getSubscriptionId())
            .name(group.getName())
            .region(group.getRegion())
            .build();
    }

    @Nonnull
    public ResourceGroup toResource() {
        final ResourceGroup rg = Azure.az(AzureResources.class).groups(this.subscriptionId).getOrDraft(this.name, this.name);
        if (rg.isDraftForCreating()) {
            final ResourceGroupDraft draft = (ResourceGroupDraft) rg;
            draft.setRegion(this.region);
        }
        return rg;
    }
}
