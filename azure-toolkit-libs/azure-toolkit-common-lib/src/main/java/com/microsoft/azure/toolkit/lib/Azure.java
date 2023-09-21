/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.azure.core.implementation.http.HttpClientProviders;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class Azure {
    @Setter
    private Consumer<AzureConfiguration> loadConfigurationHandler;
    @Setter
    private Consumer<AzureConfiguration> saveConfigurationHandler;
    private final AzureConfiguration configuration;
    private static final Azure defaultInstance = new Azure();

    private Azure() {
        this.configuration = Holder.configuration;
    }

    private static class Holder {
        private static final AzureConfiguration configuration = loadAzureConfiguration();

        @Nonnull
        private static AzureConfiguration loadAzureConfiguration() {
            final ClassLoader current = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(Azure.class.getClassLoader());
                final ServiceLoader<AzureConfigurationProvider> loader = ServiceLoader.load(AzureConfigurationProvider.class, Azure.class.getClassLoader());
                final Iterator<AzureConfigurationProvider> iterator = loader.iterator();
                if (iterator.hasNext()) {
                    return iterator.next().getConfiguration();
                }
                return new AzureConfiguration.Default();
            } finally {
                Thread.currentThread().setContextClassLoader(current);
            }
        }
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
    public static List<AzService> getServices(String provider) {
        return ServiceManager.getServices().stream().filter(s -> StringUtils.equalsIgnoreCase(provider, s.getName())).collect(Collectors.toList());
    }

    @Nonnull
    public static <T extends AzService> List<T> getServices(Class<T> clazz) {
        return ServiceManager.getServices().stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
    }

    @Nullable
    @AzureOperation(name = "internal/$resource.get.id", params = {"id"})
    public AbstractAzResource<?, ?, ?> getById(String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String provider = Optional.ofNullable(resourceId.providerNamespace()).orElse("Microsoft.Resources");
        final List<AzService> services = getServices(provider);
        AbstractAzResource<?, ?, ?> result = null;
        for (AzService service : services) {
            if (service instanceof AbstractAzService) {
                result = service.getById(id);
                if (Objects.nonNull(result)) {
                    break;
                }
            }
        }
        if (result == null) {
            log.warn(String.format("fallback to AzureResources because no valid service provider for '%s' is found.", id));
            return Azure.az(AzureResources.class).getById(id);
        }
        return result;
    }

    @Nullable
    @AzureOperation(name = "internal/$resource.get.id", params = {"id"})
    public AbstractAzResource<?, ?, ?> getOrInitById(String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String provider = Optional.ofNullable(resourceId.providerNamespace()).orElse("Microsoft.Resources");
        final List<AzService> services = getServices(provider);
        AbstractAzResource<?, ?, ?> result = null;
        for (AzService service : services) {
            if (service instanceof AbstractAzService) {
                result = service.getOrInitById(id);
                if (Objects.nonNull(result)) {
                    break;
                }
            }
        }
        if (result == null) {
            log.warn(String.format("fallback to AzureResources because no valid service provider for '%s' is found.", id));
            return Azure.az(AzureResources.class).getOrInitById(id);
        }
        return result;
    }

    public static Azure az() {
        return defaultInstance;
    }

    public AzureConfiguration config() {
        return this.configuration;
    }

    public void loadConfiguration() {
        Optional.ofNullable(this.loadConfigurationHandler).ifPresent(h -> h.accept(this.configuration));
    }

    public void saveConfiguration() {
        Optional.ofNullable(this.saveConfigurationHandler).ifPresent(h -> h.accept(this.configuration));
    }

    private static class ServiceManager {
        private static final List<AzService> services = new ArrayList<>();

        public static synchronized List<AzService> getServices() {
            if (services.isEmpty()) {
                // fix the class load problem for intellij plugin
                final ClassLoader current = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(Azure.class.getClassLoader());
                    ResourceManagerUtils.InternalRuntimeContext.setDelayProvider(duration -> Duration.ofSeconds(5));
                    HttpClientProviders.createInstance();
                    reload();
                } catch (final Throwable e) {
                    log.error(e.getMessage(), e);
                } finally {
                    Thread.currentThread().setContextClassLoader(current);
                }
            }
            return services;
        }

        public static synchronized void reload() {
            final ServiceLoader<AzService> loader = ServiceLoader.load(AzService.class, Azure.class.getClassLoader());
            loader.reload();
            services.clear();
            loader.forEach(services::add);
        }
    }
}
