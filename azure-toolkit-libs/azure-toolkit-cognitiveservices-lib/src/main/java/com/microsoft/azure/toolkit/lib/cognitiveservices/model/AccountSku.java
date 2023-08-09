/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices.model;

import com.azure.resourcemanager.cognitiveservices.models.ResourceSku;
import com.azure.resourcemanager.cognitiveservices.models.Sku;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class AccountSku {
    private String name;
    private String tier;

    public static AccountSku fromSku(Sku sku) {
        return AccountSku.builder()
            .name(sku.name())
            .tier(sku.tier().toString())
            .build();
    }

    public static AccountSku fromSku(ResourceSku sku) {
        return AccountSku.builder()
            .name(sku.name())
            .tier(sku.tier())
            .build();
    }
}
