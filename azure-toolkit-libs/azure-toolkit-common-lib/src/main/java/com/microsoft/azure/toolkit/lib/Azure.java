/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

public class Azure {
    private final AzureConfiguration configuration;
    private static final Azure defaultInstance = new Azure();

    private Azure() {
        this.configuration = new AzureConfiguration();
    }

    public static synchronized <T extends AzureService> T az(final Class<T> clazz) {
        T service = getService(clazz);
        if (service == null) {
            Holder.loader.reload();
            service = getService(clazz);
        }
        if (service != null) {
            return service;
        }
        throw new AzureToolkitRuntimeException(String.format("Azure service(%s) not supported", clazz.getSimpleName()));
    }

    @Nullable
    private static <T extends AzureService> T getService(Class<T> clazz) {
        for (AzureService service : Holder.loader) {
            if (clazz.isInstance(service)) {
                return clazz.cast(service);
            }
        }
        return null;
    }

    public static Azure az() {
        return defaultInstance;
    }

    public AzureConfiguration config() {
        return this.configuration;
    }

    private static class Holder {
        private static final ServiceLoader<AzureService> loader = ServiceLoader.load(AzureService.class);
    }
}
