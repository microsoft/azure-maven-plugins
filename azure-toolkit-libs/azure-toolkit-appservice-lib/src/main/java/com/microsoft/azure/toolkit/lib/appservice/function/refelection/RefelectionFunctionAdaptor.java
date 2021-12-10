/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.refelection;

import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotationType;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class RefelectionFunctionAdaptor {
    public static FunctionAnnotation create(@Nonnull Annotation annotation) {
        return create(annotation, true);
    }

    public static FunctionMethod create(Method method) {
        FunctionMethod functionMethod = new FunctionMethod();
        functionMethod.setName(method.getName());
        functionMethod.setReturnTypeName(method.getReturnType().getCanonicalName());
        functionMethod.setAnnotations(method.getAnnotations() == null ? Collections.emptyList() :
                Arrays.stream(method.getAnnotations()).map(RefelectionFunctionAdaptor::create).collect(Collectors.toList()));

        List<FunctionAnnotation[]> parameterAnnotations = Arrays.stream(method.getParameters())
                .map(Parameter::getAnnotations).filter(Objects::nonNull)
                .map(q -> Arrays.stream(q)
                        .map(RefelectionFunctionAdaptor::create)
                        .collect(Collectors.toList()).toArray(new FunctionAnnotation[0])).collect(Collectors.toList());

        functionMethod.setParameterAnnotations(parameterAnnotations);
        functionMethod.setDeclaringTypeName(method.getDeclaringClass().getCanonicalName());
        return functionMethod;
    }

    private static FunctionAnnotation create(@Nonnull Annotation annotation, boolean resolveAnnotationType) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> defaultMap = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                Object value = method.invoke(annotation);
                if (Objects.equals(value, method.getDefaultValue())) {
                    defaultMap.put(method.getName(), value);
                } else {
                    map.put(method.getName(), value);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AzureToolkitRuntimeException(String.format("Cannot invoke method '%s' for annotation class '%s'",
                        method.getName(),
                        annotation.getClass().getSimpleName()
                ), e);
            }
        }

        FunctionAnnotation functionAnnotation = new FunctionAnnotation() {
            public boolean isAnnotationType(@Nonnull Class<? extends Annotation> clz) {
                return clz.isInstance(annotation);
            }
        };
        if (resolveAnnotationType) {
            functionAnnotation.setAnnotationType(toFunctionAnnotationType(annotation.annotationType()));
        }
        functionAnnotation.setProperties(map);
        functionAnnotation.setDefaultProperties(defaultMap);
        return functionAnnotation;
    }

    private static FunctionAnnotationType toFunctionAnnotationType(Class<? extends Annotation> clz) {
        FunctionAnnotationType type = new FunctionAnnotationType();
        type.setFullName(clz.getCanonicalName());
        type.setName(clz.getSimpleName());
        type.setAnnotations(Arrays.stream(clz.getAnnotations()).map(a -> create(a, false)).collect(Collectors.toList()));
        return type;
    }
}
