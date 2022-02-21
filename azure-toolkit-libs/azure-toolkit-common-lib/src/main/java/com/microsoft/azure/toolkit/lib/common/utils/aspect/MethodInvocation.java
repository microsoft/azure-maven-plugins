/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils.aspect;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Triple;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MethodInvocation {
    @Getter
    @EqualsAndHashCode.Include
    protected final Method method;
    private final JoinPoint point;
    private final MethodSignature signature;

    @Builder
    MethodInvocation(JoinPoint point) {
        this.point = point;
        this.signature = (MethodSignature) this.point.getSignature();
        this.method = signature.getMethod();
    }

    public Object invoke() throws Exception {
        if (this.point instanceof ProceedingJoinPoint) {
            try {
                return ((ProceedingJoinPoint) this.point).proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable t) {
                throw new AzureToolkitException(t.getMessage(), t);
            }
        }
        return null;
    }

    public Object getInstance() {
        return this.point.getThis();
    }

    public List<Triple<String, Parameter, Object>> getArgs() {
        final List<Triple<String, Parameter, Object>> result = new ArrayList<>();
        final String[] names = this.signature.getParameterNames();
        final Parameter[] params = this.method.getParameters();
        final Object[] values = this.point.getArgs();
        for (int i = 0; i < params.length; i++) {
            result.add(Triple.of(names[i], params[i], values[i]));
        }
        return result;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return this.method.getAnnotation(annotation);
    }

    public static MethodInvocation from(@Nonnull final JoinPoint point) {
        return new MethodInvocation(point);
    }
}
