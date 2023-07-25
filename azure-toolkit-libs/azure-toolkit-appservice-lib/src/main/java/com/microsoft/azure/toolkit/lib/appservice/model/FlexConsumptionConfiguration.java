/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.fluent.models.SiteConfigInner;
import com.azure.resourcemanager.appservice.fluent.models.SiteInner;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class FlexConsumptionConfiguration {
    public static final int DEFAULT_INSTANCE_SIZE = 2048;
    public static final FlexConsumptionConfiguration DEFAULT =
        FlexConsumptionConfiguration.builder().instanceSize(DEFAULT_INSTANCE_SIZE).build();

    private Integer instanceSize;
    private Integer alwaysReadyInstances;
    private Integer maximumInstances;

    public static FlexConsumptionConfiguration fromWebAppBase(@Nonnull final WebAppBase app) {
        return FlexConsumptionConfiguration.builder()
            .instanceSize(app.containerSize())
            .alwaysReadyInstances(Optional.ofNullable(app.innerModel()).map(SiteInner::siteConfig).map(SiteConfigInner::minimumElasticInstanceCount).orElse(null))
            .maximumInstances(Optional.ofNullable(app.innerModel()).map(SiteInner::siteConfig).map(SiteConfigInner::functionAppScaleLimit).orElse(null)).build();
    }
}
