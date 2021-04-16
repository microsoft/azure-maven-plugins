/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public class Azure {
    private final AzureConfiguration configuration;
    private static final ServiceLoader<AzureService> loader = ServiceLoader.load(AzureService.class);
    private static final Azure defaultInstance = new Azure();

    private Azure() {
        this.configuration = new AzureConfiguration();
    }

    public static <T extends AzureService> T az(final Class<T> clazz) {
        return StreamSupport.stream(loader.spliterator(), false)
            .filter(clazz::isInstance).findAny().map(clazz::cast)
            .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("Azure service(%s) not supported", clazz.getSimpleName())));
    }

    public static Azure az() {
        return defaultInstance;
    }

    public AzureConfiguration config() {
        return this.configuration;
    }
}
