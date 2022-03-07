/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Azure {
    private final AzureConfiguration configuration;
    private static final Azure defaultInstance = new Azure();

    private Azure() {
        this.configuration = new AzureConfiguration();
    }

    public static synchronized <T extends AzService> T az(final Class<T> clazz) {
        T service = getService(clazz);
        if (service == null) {
            Holder.loader.reload();
            Holder.azLoader.reload();
            service = getService(clazz);
        }
        if (service != null) {
            return service;
        }
        throw new AzureToolkitRuntimeException(String.format("Azure service(%s) not supported", clazz.getSimpleName()));
    }

    @Nullable
    private static <T extends AzService> T getService(Class<T> clazz) {
        for (AzService service : Holder.azLoader) {
            if (clazz.isInstance(service)) {
                return clazz.cast(service);
            }
        }
        for (AzureService service : Holder.loader) {
            if (clazz.isInstance(service)) {
                return clazz.cast(service);
            }
        }
        return null;
    }

    @Nonnull
    public static <T extends AzService> List<T> getServices(Class<T> clazz) {
        final List<T> result = new ArrayList<>();
        for (AzService service : Holder.azLoader) {
            if (clazz.isInstance(service)) {
                result.add(clazz.cast(service));
            }
        }
        for (AzureService service : Holder.loader) {
            if (clazz.isInstance(service)) {
                result.add(clazz.cast(service));
            }
        }
        return result;
    }

    public static Azure az() {
        return defaultInstance;
    }

    public AzureConfiguration config() {
        return this.configuration;
    }

    private static class Holder {
        private static final ServiceLoader<AzService> azLoader = ServiceLoader.load(AzService.class, Azure.class.getClassLoader());
        private static final ServiceLoader<AzureService> loader = ServiceLoader.load(AzureService.class, Azure.class.getClassLoader());
    }
}
