/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.utils;

import java.lang.annotation.Annotation;

public class AnnotationUtils {
    public static <T> T getNotDefaultValueFromAnnotation(T input, String name, Annotation annotation) {
        T defaultValue = null;
        try {
            defaultValue = (T) annotation.annotationType().getMethod(name).getDefaultValue();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return input.equals(defaultValue) ? null : input;
    }
}
