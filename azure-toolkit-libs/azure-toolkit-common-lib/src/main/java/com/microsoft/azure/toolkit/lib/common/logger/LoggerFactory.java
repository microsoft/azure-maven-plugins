/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.logger;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.ServiceLoader;

public abstract class LoggerFactory {
    private static final ServiceLoader<LoggerFactory> loader = ServiceLoader.load(LoggerFactory.class, LoggerFactory.class.getClassLoader());
    @Nullable
    private static LoggerFactory instance;

    public synchronized static LoggerFactory getInstance() {
        if (Objects.isNull(instance)) {
            loader.reload();
            for (LoggerFactory factory : loader) {
                LoggerFactory.instance = factory;
            }
        }
        if (Objects.isNull(LoggerFactory.instance)) {
            throw new AzureToolkitRuntimeException("no LoggerFactory registered.");
        }
        return LoggerFactory.instance;
    }

    public static synchronized void register(LoggerFactory factory) {
        LoggerFactory.instance = factory;
    }

    public abstract Logger getLogger(Class<?> clazz);
}
