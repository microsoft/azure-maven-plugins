/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.entity;

import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class FunctionAppEntity extends AppServiceBaseEntity {
    private IFunctionApp functionApp;

    @Override
    public Map<String, String> getAppSettings() {
        return functionApp.appSettings();
    }
}
