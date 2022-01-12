/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource.Status;
import org.apache.commons.lang3.StringUtils;

public interface Startable {

    void start();

    void stop();

    void restart();

    default boolean isStartable() {
        return StringUtils.equalsIgnoreCase(this.status(), Status.STOPPED);
    }

    default boolean isStoppable() {
        return StringUtils.equalsAnyIgnoreCase(this.status(), Status.RUNNING);
    }

    default boolean isRestartable() {
        return this.isStoppable();
    }

    String status();
}
