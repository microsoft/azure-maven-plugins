/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.MethodInvocation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Objects;
import java.util.concurrent.Callable;

@Aspect
@Slf4j
public final class AzureOperationAspect {

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.operation.AzureOperation * *..*.*(..))")
    public void operation() {
    }

    @Before("operation()")
    public void beforeEnter(JoinPoint point) {
        beforeEnter(toOperation(point));
    }

    @AfterReturning("operation()")
    public void afterReturning(JoinPoint point) {
        afterReturning(toOperation(point));
    }

    @AfterThrowing(pointcut = "operation()", throwing = "e")
    public void afterThrowing(JoinPoint point, Throwable e) throws Throwable {
        afterThrowing(e, toOperation(point));
    }

    //    @Around("operation()")
    //    public Object around(ProceedingJoinPoint point) throws Throwable {
    //        final IAzureOperation current = toOperation(point);
    //        final Object source = point.getThis();
    //        return execute(current, source);
    //    }

    public static void beforeEnter(Operation operation) {
        final Object source = operation.getSource();
        if (source instanceof AzResourceModule) {
            operation.getContext().setTelemetryProperty("subscriptionId", ((AzResourceModule<?>) source).getSubscriptionId());
        } else if (source instanceof AzResource) {
            operation.getContext().setTelemetryProperty("subscriptionId", ((AzResource) source).getSubscriptionId());
        }
        AzureTelemeter.beforeEnter(operation);
        OperationManager.getInstance().fireBeforeEnter(operation);
        OperationThreadContext.current().pushOperation(operation);
    }

    public static void afterReturning(Operation current) {
        final Operation operation = OperationThreadContext.current().popOperation();
        if (operation == null) { // @wangmi FIXME: just workaround
            return;
        }
        // TODO: this cannot ensure same operation actually, considering recursive call
        assert Objects.equals(current, operation) :
            String.format("popped operation[%s] is not the exiting operation[%s]", current, operation);
        OperationManager.getInstance().fireAfterReturning(operation);
        AzureTelemeter.afterExit(operation);
    }

    public static void afterThrowing(Throwable e, Operation current) throws Throwable {
        final Operation operation = OperationThreadContext.current().popOperation();
        if (operation == null) { // @wangmi FIXME: just workaround
            return;
        }
        // TODO: this cannot ensure same operation actually, considering recursive call
        assert Objects.equals(current, operation) :
            String.format("popped operation[%s] is not the operation[%s] throwing exception", current, operation);
        if (e instanceof OperationException) {
            throw e;
        } else {
            OperationManager.getInstance().fireAfterThrowing(e, operation);
            AzureTelemeter.onError(operation, e);
            if (e instanceof Exception && !(e instanceof RuntimeException)) {
                throw e; // do not wrap checked exception and AzureOperationException
            }
            throw new OperationException(operation, e);
        }
    }

    public static <T> T execute(Operation operation) throws Throwable {
        final Callable<?> body = operation.getBody();
        try {
            AzureOperationAspect.beforeEnter(operation);
            //noinspection unchecked
            final T result = (T) body.call();
            AzureOperationAspect.afterReturning(operation);
            return result;
        } catch (final Throwable e) {
            AzureOperationAspect.afterThrowing(e, operation);
            throw e;
        }
    }

    private static Operation toOperation(JoinPoint point) {
        return new MethodOperation(MethodInvocation.from(point));
    }
}
