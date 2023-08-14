/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices.model;

import com.azure.resourcemanager.cognitiveservices.models.Sku;
import com.azure.resourcemanager.cognitiveservices.models.SkuTier;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

@Data
@Builder
@EqualsAndHashCode
public class DeploymentSku {
    private String name;
    private String tier;
    private String size;
    private String family;
    private Integer capacity;

    public static DeploymentSku fromSku(@Nonnull final Sku sku) {
        return DeploymentSku.builder()
            .name(sku.name())
            .tier(sku.tier().toString())
            .size(sku.size())
            .family(sku.family())
            .capacity(sku.capacity())
            .build();
    }

    public static DeploymentSku fromModelSku(@Nonnull final ModelSku sku) {
        return DeploymentSku.builder()
            .name(sku.getName())
            .capacity(Optional.ofNullable(sku.getCapacityConfig()).map(ModelSku.Capacity::getDefaultProperty).orElse(1))
            .build();
    }

    public Sku toSku() {
        return new Sku().withName(name)
            .withTier(Optional.ofNullable(tier).filter(StringUtils::isNoneEmpty).map(SkuTier::fromString).orElse(null))
            .withSize(size)
            .withFamily(family)
            .withCapacity(capacity);
    }
}
