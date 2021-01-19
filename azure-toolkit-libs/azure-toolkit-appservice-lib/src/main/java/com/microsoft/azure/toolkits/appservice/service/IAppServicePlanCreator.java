/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;


import com.azure.resourcemanager.appservice.models.AppServicePlan;

public interface IAppServicePlanCreator {
    IAppServicePlanCreator withResourceGroup(String name);

    AppServicePlan create();
}
