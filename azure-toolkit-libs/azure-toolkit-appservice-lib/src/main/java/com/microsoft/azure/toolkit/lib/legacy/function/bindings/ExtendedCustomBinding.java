/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.bindings;


import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.impl.DefaultFunctionProject;

import java.lang.annotation.Annotation;

public class ExtendedCustomBinding extends Binding {

    private FunctionAnnotation customBindingAnnotation;

    public ExtendedCustomBinding(BindingEnum bindingEnum,
                                 Annotation customBindingAnnotation,
                                 Annotation annotation) {
        super(bindingEnum, annotation);
        this.customBindingAnnotation = DefaultFunctionProject.create(customBindingAnnotation);
    }

    @Override
    public String getName() {
        final String name = super.getName();
        if (name != null) {
            return name;
        }
        return customBindingAnnotation.getStringValue("name", true);
    }

    @Override
    public String getDirection() {
        if (this.direction != null) {
            return direction.toString();
        }
        return customBindingAnnotation.getStringValue("direction", true);
    }

    @Override
    public String getType() {
        if (type != null) {
            return type;
        }
        return customBindingAnnotation.getStringValue("type", true);
    }
}
