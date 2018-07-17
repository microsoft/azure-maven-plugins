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
    public static AppServicePlan getAppServicePlan(final AbstractAppServiceMojo mojo)
            throws Exception {
        final String servicePlanName = mojo.getAppServicePlanName();
        if (StringUtils.isNotEmpty(servicePlanName)) {
            final String servicePlanResGrp = getAppServicePlanResourceGroup(mojo);
            return mojo.getAzureClient().appServices().appServicePlans()
                    .getByResourceGroup(servicePlanResGrp, servicePlanName);
        }
        return null;
    }

    public static String getAppServicePlanResourceGroup(final AbstractAppServiceMojo mojo) {
        final String defaultResourceGroup = mojo.getResourceGroup();
        
        return StringUtils.isEmpty(mojo.getAppServicePlanResourceGroup()) ?
            defaultResourceGroup : mojo.getAppServicePlanResourceGroup();
    }
}
