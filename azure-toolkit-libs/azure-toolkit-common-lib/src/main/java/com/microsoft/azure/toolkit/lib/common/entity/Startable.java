/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import java.util.Objects;

public interface Startable<T extends IAzureResourceEntity> extends IAzureResource<T> {

    void start();

    void stop();

    void restart();

    default boolean isStartable() {
        return Objects.equals(this.status(), Status.STOPPED);
    }

    default boolean isStoppable() {
        return Objects.equals(this.status(), Status.RUNNING);
    }

    default boolean isRestartable() {
        return Objects.equals(this.status(), Status.RUNNING);
    }
}
