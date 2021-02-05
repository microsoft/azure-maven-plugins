/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import lombok.extern.java.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;

@Aspect
@Log
public final class AzureOperationAspect {

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.operation.AzureOperation * *..*.*(..))")
    public void operation() {
    }

    @Before("operation()")
    public void beforeEnter(JoinPoint point) {
        enterOperation(point);
    }

    @AfterReturning("operation()")
    public void afterReturning(JoinPoint point) {
        exitOperation(point);
    }

    @AfterThrowing(pointcut = "operation()", throwing = "e")
    public void afterThrowing(JoinPoint point, Throwable e) throws Throwable {
        final AzureOperationRef operation = exitOperation(point);
        if (!(e instanceof RuntimeException)) {
            throw e; // do not wrap checked exception
        }
        throw new AzureOperationException(operation, e);
    }

    private static AzureOperationRef enterOperation(JoinPoint point) {
        final AzureOperationRef operation = toOperationRef(point);
        AzureTelemeter.beforeEnter(operation);
        AzureTaskContext.current().pushOperation(operation);
        return operation;
    }

    private static AzureOperationRef exitOperation(JoinPoint point) {
        final AzureOperationRef current = toOperationRef(point);
        final AzureOperationRef operation = (AzureOperationRef) AzureTaskContext.current().popOperation();
        AzureTelemeter.afterExit(operation);
        assert Objects.equals(current, operation) : String.format("popped operation[%s] is not the exiting operation[%s]", current, operation);
        return operation;
    }

    private static AzureOperationRef toOperationRef(JoinPoint point) {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Object[] args = point.getArgs();
        final Object instance = point.getThis();
        return AzureOperationRef.builder()
            .instance(instance)
            .method(signature.getMethod())
            .paramNames(signature.getParameterNames())
            .paramValues(args)
            .build();
    }
}
