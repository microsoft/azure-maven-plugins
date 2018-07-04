/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import org.codehaus.plexus.util.StringUtils;

public class AppServiceUtils {
    public static AppServicePlan getExistingAppServicePlan(final AbstractAppServiceMojo mojo)
            throws Exception {
        final String servicePlanResGrp = getAppServicePlanResourceGroup(mojo);

        final String servicePlanName = mojo.getAppServicePlanName();
        if (StringUtils.isNotEmpty(servicePlanName)) {
            return mojo.getAzureClient().appServices().appServicePlans()
                    .getByResourceGroup(servicePlanResGrp, servicePlanName);
        }
        return null;
    }

    public static String getAppServicePlanResourceGroup(final AbstractAppServiceMojo mojo) {
        return StringUtils.isNotEmpty(mojo.getAppServicePlanResourceGroup()) ?
                mojo.getAppServicePlanResourceGroup() : mojo.getResourceGroup();
    }
}
