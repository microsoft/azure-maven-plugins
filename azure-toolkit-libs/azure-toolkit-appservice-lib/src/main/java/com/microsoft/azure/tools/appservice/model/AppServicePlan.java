/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.tools.appservice.model;

import com.azure.resourcemanager.appservice.models.OperatingSystem;
import com.azure.resourcemanager.appservice.models.PricingTier;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder(toBuilder = true)
public class AppServicePlan {
    private String id;
    private String name;
    private String region;
    private ResourceGroup resourceGroup;
    private PricingTier pricingTier;
    private OperatingSystem operatingSystem;

    public static boolean equals(AppServicePlan first, AppServicePlan second) {
        return StringUtils.equals(first.getId(), second.getId()) ||
                StringUtils.equals(first.getResourceGroup().getName(), second.getResourceGroup().getName()) &&
                        StringUtils.equals(first.getName(), second.getName());
    }
}
