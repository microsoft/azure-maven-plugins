/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import java.lang.annotation.Annotation;

public class CustomBinding extends Binding {

    private com.microsoft.azure.functions.annotation.CustomBinding customBindingAnnotation;

    public CustomBinding(BindingEnum bindingEnum,
                         com.microsoft.azure.functions.annotation.CustomBinding customBindingAnnotation,
                         Annotation annotation) {
        super(bindingEnum, annotation);
        this.customBindingAnnotation = customBindingAnnotation;
        this.bindingAttributes.put("name", customBindingAnnotation.name());
    }

    @Override
    public String getDirection() {
        return customBindingAnnotation.direction();
    }

    @Override
    public String getType() {
        return customBindingAnnotation.type();
    }
}
