/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.service.serviceplan.AppServicePlanUpdatable;
import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;

public interface AppServicePlanManager {
    boolean exists();

    AppServicePlan get();

    AppServicePlanUpdatable update();

    com.azure.resourcemanager.appservice.models.AppServicePlan getPlanService();
}
