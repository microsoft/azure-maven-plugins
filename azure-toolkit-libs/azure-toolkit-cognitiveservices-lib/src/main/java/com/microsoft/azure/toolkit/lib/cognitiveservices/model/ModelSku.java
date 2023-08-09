/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices.model;

import com.azure.resourcemanager.cognitiveservices.models.CapacityConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;

@Data
@Builder
@EqualsAndHashCode
public class ModelSku {
    private String name;
    private String usageName;
    private Capacity capacityConfig;

    public static ModelSku fromModelSku(@Nonnull final com.azure.resourcemanager.cognitiveservices.models.ModelSku sku) {
        return ModelSku.builder()
            .name(sku.name())
            .usageName(sku.usageName())
            .capacityConfig(Capacity.fromCapacityConfig(sku.capacity()))
            .build();
    }

    @Data
    @Builder
    @EqualsAndHashCode
    public static class Capacity {
        private Integer minimum;
        private Integer maximum;
        private Integer defaultProperty;

        public static Capacity fromCapacityConfig(@Nonnull final CapacityConfig capacityConfig) {
            return Capacity.builder()
                .minimum(capacityConfig.minimum())
                .maximum(capacityConfig.maximum())
                .defaultProperty(capacityConfig.defaultProperty())
                .build();
        }
    }
}
