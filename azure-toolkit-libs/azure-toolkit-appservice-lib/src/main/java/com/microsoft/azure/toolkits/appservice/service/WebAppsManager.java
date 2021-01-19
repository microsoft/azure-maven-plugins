/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.WebApp;

import java.util.List;

public interface WebAppsManager {
    AppServiceCreatable.WithName create();

    WebApp get(String id);

    WebApp get(String resourceGroup, String name);

    List<WebApp> list();
}
