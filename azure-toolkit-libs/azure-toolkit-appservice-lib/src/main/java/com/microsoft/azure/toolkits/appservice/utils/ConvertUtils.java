/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.utils;

import com.azure.resourcemanager.appservice.models.AppSetting;

import java.util.Map;
import java.util.stream.Collectors;

public class ConvertUtils {
    public static Map<String, String> normalizeAppSettings(Map<String, AppSetting> input) {
        return input.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().value()));
    }
}
