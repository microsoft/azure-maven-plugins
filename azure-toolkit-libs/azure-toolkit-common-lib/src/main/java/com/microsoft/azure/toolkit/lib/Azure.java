/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Azure {
    private final AzureConfiguration configuration;
    private static final Azure defaultInstance = new Azure();

    private Azure() {
        this.configuration = new AzureConfiguration();
    }

    public static synchronized <T extends AzService> T az(final Class<T> clazz) {
        final T service = Optional.ofNullable(getService(clazz)).orElseGet(() -> {
            ServiceManager.reload();
            return getService(clazz);
        });
        final String message = String.format("Azure service(%s) not supported", clazz.getSimpleName());
        return Optional.ofNullable(service).orElseThrow(() -> new AzureToolkitRuntimeException(message));
    }

    @Nullable
    private static <T extends AzService> T getService(Class<T> clazz) {
        return ServiceManager.getServices().stream().filter(clazz::isInstance).map(clazz::cast).findAny().orElse(null);
    }

    @Nullable
    private static AzService getService(String provider) {
        return ServiceManager.getServices().stream().filter(s -> StringUtils.equalsIgnoreCase(provider, s.getName())).findAny().orElse(null);
    }

    @Nonnull
    public static <T extends AzService> List<T> getServices(Class<T> clazz) {
        return ServiceManager.getServices().stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
    }

    @AzureOperation(name = "resource.get.id", params = {"id"}, type = AzureOperation.Type.SERVICE)
    public AbstractAzResource<?, ?, ?> getById(String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String provider = Optional.ofNullable(resourceId.providerNamespace()).orElse("Microsoft.Resources");
        final AzService service = getService(provider);
        if (service instanceof AbstractAzService) {
            return ((AbstractAzService<?, ?>) service).getById(id);
        }
        throw new AzureToolkitRuntimeException("can not find a valid service provider!");
    }

    public static Azure az() {
        return defaultInstance;
    }

    public AzureConfiguration config() {
        return this.configuration;
    }

    private static class ServiceManager {
        private static final ServiceLoader<AzService> azLoader = ServiceLoader.load(AzService.class, Azure.class.getClassLoader());
        private static final ServiceLoader<AzureService> loader = ServiceLoader.load(AzureService.class, Azure.class.getClassLoader());
        private static final List<AzService> services = new ArrayList<>();

        public static synchronized List<AzService> getServices() {
            if (services.isEmpty()) {
                reload();
            }
            return services;
        }

        public static synchronized void reload() {
            ServiceManager.loader.reload();
            ServiceManager.azLoader.reload();
            services.clear();
            ServiceManager.azLoader.forEach(services::add);
            ServiceManager.loader.forEach(services::add);
        }
    }
}
