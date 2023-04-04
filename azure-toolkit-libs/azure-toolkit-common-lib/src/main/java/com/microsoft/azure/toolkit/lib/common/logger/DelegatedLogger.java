/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.logger;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
class DelegatedLogger implements Logger {
    private final Class<?> clazz;
    private Logger delegate;

    @Delegate
    private Logger getDelegate() {
        if (Objects.isNull(delegate)) {
            synchronized (this) {
                if (Objects.isNull(delegate)) {
                    final LoggerFactory factory = LoggerFactory.getInstance();
                    if (Objects.isNull(factory)) {
                        System.out.println("###################################################");
                        System.out.println("no LoggerFactory registered. fallback to SoutLogger.");
                        System.out.println("###################################################");
                        this.delegate = new SoutLogger(clazz);
                    } else {
                        this.delegate = factory.getLogger(clazz);
                    }
                }
            }
        }

        return this.delegate;
    }

    @RequiredArgsConstructor
    static class SoutLogger implements Logger {
        private final Class<?> clazz;

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String message, @Nullable Throwable t) {

        }

        @Override
        public void info(String message, @Nullable Throwable t) {
            System.out.printf("%s - [INFO] %s - %s%n", new Date(), clazz.getName(), message);
            System.out.printf("%s - [INFO] %s - %n", new Date(), clazz.getName());
            Optional.ofNullable(t).ifPresent(Throwable::printStackTrace);
        }

        @Override
        public void warn(String message, @Nullable Throwable t) {
            System.out.printf("%s - [WARN] %s - %s%n", new Date(), clazz.getName(), message);
            System.out.printf("%s - [WARN] %s - %n", new Date(), clazz.getName());
            Optional.ofNullable(t).ifPresent(Throwable::printStackTrace);
        }

        @Override
        public void error(String message, @Nullable Throwable t) {
            System.out.printf("%s - [ERROR] %s - %s%n", new Date(), clazz.getName(), message);
            System.out.printf("%s - [ERROR] %s - %n", new Date(), clazz.getName());
            Optional.ofNullable(t).ifPresent(Throwable::printStackTrace);
        }
    }
}
