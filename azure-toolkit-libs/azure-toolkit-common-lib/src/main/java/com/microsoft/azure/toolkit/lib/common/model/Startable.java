/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

public interface Startable extends AzResource {

    void start();

    void stop();

    void restart();

    default boolean isStartable() {
        return this.getFormalStatus(true).isStopped();
    }

    default boolean isStoppable() {
        return this.getFormalStatus(true).isRunning();
    }

    default boolean isRestartable() {
        return this.isStoppable();
    }
}
