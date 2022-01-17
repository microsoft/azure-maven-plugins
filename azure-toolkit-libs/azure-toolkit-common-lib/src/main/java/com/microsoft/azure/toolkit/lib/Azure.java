/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public class Azure {
    private final AzureConfiguration configuration;
    private ClassLoader classLoader;
    private static final Azure defaultInstance = new Azure();

    private Azure() {
        this.configuration = new AzureConfiguration();
    }

    public void setClassLoader(@Nonnull ClassLoader classLoader) {
        if (!Objects.equals(classLoader, this.classLoader)) {
            this.classLoader = classLoader;
            Holder.azLoader = ServiceLoader.load(AzService.class, this.classLoader);
            Holder.loader = ServiceLoader.load(AzureService.class, this.classLoader);
        }
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
        private static ServiceLoader<AzService> azLoader = ServiceLoader.load(AzService.class);
        private static ServiceLoader<AzureService> loader = ServiceLoader.load(AzureService.class);
    }
}
