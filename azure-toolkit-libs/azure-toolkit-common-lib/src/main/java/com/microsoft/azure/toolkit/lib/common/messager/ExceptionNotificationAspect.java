/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import lombok.extern.java.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.stream.Stream;

@Aspect
@Log
public final class ExceptionNotificationAspect {

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification * *..*.*(..))")
    public void onException() {
    }

    @AfterThrowing(pointcut = "onException()", throwing = "e")
    public void afterThrowing(JoinPoint point, Throwable e) throws Throwable {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final Method method = signature.getMethod();
        final ExceptionNotification annotation = method.getAnnotation(ExceptionNotification.class);
        final Class<Throwable>[] classes = annotation.value();
        if (Stream.of(classes).anyMatch(c -> c.isInstance(e))) {
            AzureMessager.getMessager().error(e);
            if (!annotation.throwAgain()) {
                return;
            }
        }
        throw e;
    }
}
