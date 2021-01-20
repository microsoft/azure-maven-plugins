/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;

public interface IAppServicePlan {
    IAppServicePlanCreator create();

    IAppServicePlanUpdater update();

    boolean exists();

    AppServicePlanEntity entity();
}
