/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class FlexConsumptionConfiguration {
    @Nonnull
    private Integer instanceSize;
    private Integer alwaysReadyInstances;
    private Integer maximumInstances;

    public static FlexConsumptionConfiguration fromWebAppBase(@Nonnull final WebAppBase app) {
        return FlexConsumptionConfiguration.builder()
            .instanceSize(app.containerSize())
            .alwaysReadyInstances(app.innerModel().siteConfig().minimumElasticInstanceCount())
            .maximumInstances(app.innerModel().siteConfig().functionAppScaleLimit()).build();
    }
}
