/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.exception;

import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
public abstract class AzureExceptionHandler {
    private static AzureExceptionHandler handler;

    public static synchronized void register(AzureExceptionHandler handler) {
        if (AzureExceptionHandler.handler == null) {
            AzureExceptionHandler.handler = handler;
        }
    }

    public static AzureExceptionHandler getInstance() {
        return AzureExceptionHandler.handler;
    }

    public static void onUncaughtException(final Throwable e) {
        AzureExceptionHandler.getInstance().handleException(e);
    }

    public static void notify(final Throwable e, @Nullable AzureExceptionAction... actions) {
        AzureExceptionHandler.getInstance().handleException(e, actions);
    }

    public static void notify(final Throwable e, boolean background, @Nullable AzureExceptionAction... actions) {
        AzureExceptionHandler.getInstance().handleException(e, background, actions);
    }

    public static void onRxException(final Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureExceptionHandler.getInstance().handleException(e);
    }

    public void handleException(Throwable throwable, @Nullable AzureExceptionAction... action) {
        log.log(Level.WARNING, "caught an error in AzureExceptionHandler", throwable);
        final Boolean backgrounded = Optional.ofNullable(AzureTaskContext.current().getTask()).map(AzureTask::getBackgrounded).orElse(null);
        if (Objects.nonNull(backgrounded)) {
            onHandleException(throwable, backgrounded, action);
            return;
        }
        onHandleException(throwable, action);
    }

    public void handleException(Throwable throwable, boolean isBackGround, @Nullable AzureExceptionAction... action) {
        log.log(Level.WARNING, "caught an error in AzureExceptionHandler", throwable);
        this.onHandleException(throwable, isBackGround, action);
    }

    protected abstract void onHandleException(Throwable throwable, @Nullable AzureExceptionAction[] action);

    protected abstract void onHandleException(Throwable throwable, boolean isBackGround, @Nullable AzureExceptionAction[] action);

    public interface AzureExceptionAction {
        String name();

        void actionPerformed(Throwable throwable);

        static AzureExceptionAction simple(String name, Consumer<? super Throwable> consumer) {
            return new AzureExceptionAction() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public void actionPerformed(final Throwable throwable) {
                    consumer.accept(throwable);
                }
            };
        }
    }

}
