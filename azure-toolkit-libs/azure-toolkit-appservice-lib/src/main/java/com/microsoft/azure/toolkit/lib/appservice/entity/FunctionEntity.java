/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.entity;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder
public class FunctionEntity {
    private final String triggerId;
    private final String functionAppId;
    private final String name;
    private final String scriptFile;
    private final String entryPoint;
    private final String triggerUrl;
    private final List<BindingEntity> bindingList;

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class BindingEntity {
        private final String type;
        private final String direction;
        private final String name;
        private final Map<String, ?> properties;
    }
}
