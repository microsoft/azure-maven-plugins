/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.AppServicePlan;

import java.util.List;

public interface AppServicePlansManager {
    // todo: add service plan creation interface

    AppServicePlan get(String id);

    AppServicePlan get(String resourceGroup, String name);

    List<AppServicePlan> list();
}
