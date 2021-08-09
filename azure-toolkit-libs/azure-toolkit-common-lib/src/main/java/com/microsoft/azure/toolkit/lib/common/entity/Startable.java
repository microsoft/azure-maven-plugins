/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

public interface Startable<T extends IAzureResourceEntity> extends IAzureResource<T> {

    void start();

    void stop();

    void restart();

    default boolean isStartable() {
        return true;
    }

    default boolean isStoppable() {
        return true;
    }

    default boolean isRestartable() {
        return true;
    }
}
