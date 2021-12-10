/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.core;

import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import lombok.Getter;

public class ExtendedCustomBinding extends Binding {
    @Getter
    private final String name;

    public ExtendedCustomBinding(String name, String direction, String type) {
        super(BindingEnum.CustomBinding);
        this.name = name;
        this.direction = BindingEnum.Direction.fromString(direction);
        this.type = type;
    }
}
