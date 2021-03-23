/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils.aspect;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

@Getter
@SuperBuilder
public class MethodInvocation {
    protected final Method method;
    protected final String[] paramNames;
    protected final Object[] paramValues;
    protected final Object instance;

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof MethodInvocation)) {
            return false;
        }
        final MethodInvocation operation = (MethodInvocation) obj;
        return Objects.equals(operation.getMethod(), this.getMethod());
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return this.method.getAnnotation(annotation);
    }

    public static MethodInvocation from(@Nonnull JoinPoint point) {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Object[] args = point.getArgs();
        final Object instance = point.getThis();
        return MethodInvocation.builder()
                .instance(instance)
                .method(signature.getMethod())
                .paramNames(signature.getParameterNames())
                .paramValues(args)
                .build();
    }
}
