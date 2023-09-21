/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class AzureMessager {

    private static final class Holder {
        private static final IAzureMessager defaultMessager = loadMessager();

        @Nonnull
        private static IAzureMessager loadMessager() {
            final ClassLoader current = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(AzureMessager.class.getClassLoader());
                // don't use "IAzureMessager" as SPI interface to be compatible with IntelliJ's "Service" mechanism.
                final ServiceLoader<AzureMessagerProvider> loader = ServiceLoader.load(AzureMessagerProvider.class, AzureMessager.class.getClassLoader());
                final Iterator<AzureMessagerProvider> iterator = loader.iterator();
                if (iterator.hasNext()) {
                    return iterator.next().getMessager();
                }
                return new DummyMessager();
            } finally {
                Thread.currentThread().setContextClassLoader(current);
            }
        }
    }

    @Nonnull
    public static IAzureMessager getDefaultMessager() {
        return Holder.defaultMessager;
    }

    @Nullable
    private static IAzureMessager loadMessager() {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AzureMessager.class.getClassLoader());
            // don't use "IAzureMessager" as SPI interface to be compatible with IntelliJ's "Service" mechanism.
            final ServiceLoader<AzureMessagerProvider> loader = ServiceLoader.load(AzureMessagerProvider.class, AzureMessager.class.getClassLoader());
            final Iterator<AzureMessagerProvider> iterator = loader.iterator();
            if (iterator.hasNext()) {
                return iterator.next().getMessager();
            }
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Nonnull
    public static IAzureMessager getMessager() {
        return OperationContext.current().getMessager();
    }

    @Slf4j
    public static class DummyMessager implements IAzureMessager {
        @Override
        public boolean show(IAzureMessage message) {
            log.info(message.getContent());
            return false;
        }
    }
}
