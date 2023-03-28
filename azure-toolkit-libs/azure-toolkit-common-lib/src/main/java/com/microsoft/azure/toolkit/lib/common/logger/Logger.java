/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Logger {
    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    default void debug(String message) {
        debug(message, (Throwable) null);
    }

    default void debug(@Nonnull Throwable t) {
        debug(t.getMessage(), t);
    }

    void debug(String message, @Nullable Throwable t);

    default void info(String message) {
        info(message, null);
    }

    default void info(@Nonnull Throwable t) {
        info(t.getMessage(), t);
    }

    void info(String message, @Nullable Throwable t);

    default void warn(String message) {
        warn(message, null);
    }

    default void warn(@Nonnull Throwable t) {
        warn(t.getMessage(), t);
    }

    void warn(String message, @Nullable Throwable t);

    default void error(String message) {
        error(message, new Throwable(message));
    }

    default void error(@Nonnull Throwable t) {
        error(t.getMessage(), t);
    }

    void error(String message, @Nullable Throwable t);

    static Logger getInstance(Class<?> clazz) {
        return new DelegatedLogger(clazz);
    }
}
