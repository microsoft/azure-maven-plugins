/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import lombok.extern.java.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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
        final IAzureOperation<?> operation = toOperation(point);
        final Object source = point.getThis();
        beforeEnter(operation, source);
    }

    @AfterReturning("operation()")
    public void afterReturning(JoinPoint point) {
        final IAzureOperation<?> current = toOperation(point);
        final Object source = point.getThis();
        afterReturning(current, source);
    }

    @AfterThrowing(pointcut = "operation()", throwing = "e")
    public void afterThrowing(JoinPoint point, Throwable e) throws Throwable {
        final IAzureOperation<?> current = toOperation(point);
        final Object source = point.getThis();
        afterThrowing(e, current, source);
    }

    public static void beforeEnter(IAzureOperation<?> operation, Object source) {
        AzureTelemeter.beforeEnter(operation);
        AzureTaskContext.current().pushOperation(operation);
        if (source instanceof AzureOperationEvent.Source) {
            final AzureOperationEvent.Source<?> target = ((AzureOperationEvent.Source<?>) source).getEventSource();
            final AzureOperationEvent<?> event = new AzureOperationEvent(target, operation, AzureOperationEvent.Stage.BEFORE);
            AzureEventBus.emit(operation.getName(), event);
        }
    }

    public static void afterReturning(IAzureOperation<?> current, Object source) {
        final IAzureOperation<?> operation = AzureTaskContext.current().popOperation();
        // TODO: this cannot ensure same operation actually, considering recursive call
        assert Objects.nonNull(operation) && Objects.equals(current, operation) :
            String.format("popped operation[%s] is not the exiting operation[%s]", current, operation);
        AzureTelemeter.afterExit(operation);
        if (source instanceof AzureOperationEvent.Source) {
            final AzureOperationEvent.Source<?> target = ((AzureOperationEvent.Source<?>) source).getEventSource();
            final AzureOperationEvent<?> event = new AzureOperationEvent(target, operation, AzureOperationEvent.Stage.AFTER);
            AzureEventBus.emit(operation.getName(), event);
        }
    }

    public static void afterThrowing(Throwable e, IAzureOperation<?> current, Object source) throws Throwable {
        final IAzureOperation<?> operation = AzureTaskContext.current().popOperation();
        // TODO: this cannot ensure same operation actually, considering recursive call
        assert Objects.nonNull(operation) && Objects.equals(current, operation) :
            String.format("popped operation[%s] is not the operation[%s] throwing exception", current, operation);
        AzureTelemeter.onError(operation, e);
        if (source instanceof AzureOperationEvent.Source) {
            final AzureOperationEvent.Source<?> target = ((AzureOperationEvent.Source<?>) source).getEventSource();
            final AzureOperationEvent<?> event = new AzureOperationEvent(target, operation, AzureOperationEvent.Stage.ERROR);
            AzureEventBus.emit(operation.getName(), event);
        }
        if (e instanceof AzureOperationException || (e instanceof Exception && !(e instanceof RuntimeException))) {
            throw e; // do not wrap checked exception and AzureOperationException
        }
        throw new AzureOperationException(operation, e);
    }

    private static IAzureOperation<?> toOperation(JoinPoint point) {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Object[] args = point.getArgs();
        final Object instance = point.getThis();
        return AnnotationOperation.builder()
            .instance(instance)
            .method(signature.getMethod())
            .paramNames(signature.getParameterNames())
            .paramValues(args)
            .build();
    }
}
