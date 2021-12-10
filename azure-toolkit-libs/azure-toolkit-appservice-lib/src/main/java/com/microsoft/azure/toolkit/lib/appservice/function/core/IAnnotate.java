/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.core;

import java.lang.annotation.Annotation;
import java.util.List;

public interface IAnnotate {
    List<FunctionAnnotation> getAnnotations();

    default FunctionAnnotation getAnnotation(Class<? extends Annotation> clz) {
        return getAnnotations().stream().filter(annotation -> annotation.isAnnotationType(clz)).findFirst().orElse(null);
    }
}
