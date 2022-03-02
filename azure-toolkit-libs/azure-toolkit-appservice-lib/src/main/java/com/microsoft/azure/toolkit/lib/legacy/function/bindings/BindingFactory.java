/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.bindings;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Locale;

import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.CUSTOM_BINDING;

@Deprecated
public class BindingFactory {
    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";

    public static Binding getBinding(final Annotation annotation) {
        final BindingEnum annotationEnum = Arrays.stream(BindingEnum.values())
            .filter(bindingEnum -> bindingEnum.name().toLowerCase(Locale.ENGLISH)
                .equals(annotation.annotationType().getSimpleName().toLowerCase(Locale.ENGLISH)))
            .findFirst().orElse(null);
        return annotationEnum == null ? getUserDefinedBinding(annotation) : new Binding(annotationEnum, annotation);
    }

    public static Binding getUserDefinedBinding(final Annotation annotation) {
        final Annotation customBindingAnnotation = Arrays.stream(annotation.annotationType().getDeclaredAnnotations())
                .filter(declaredAnnotation -> StringUtils.equals(declaredAnnotation.annotationType().getCanonicalName(), CUSTOM_BINDING))
                .findFirst().orElse(null);
        return customBindingAnnotation == null ? null : new ExtendedCustomBinding(BindingEnum.ExtendedCustomBinding,
                customBindingAnnotation, annotation);
    }

    public static Binding getHTTPOutBinding() {
        final Binding result = new Binding(BindingEnum.HttpOutput);
        result.setName(HTTP_OUTPUT_DEFAULT_NAME);
        return result;
    }
}
