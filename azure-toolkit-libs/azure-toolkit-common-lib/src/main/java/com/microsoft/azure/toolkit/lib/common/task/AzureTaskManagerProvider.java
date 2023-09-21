/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.task;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface AzureTaskManagerProvider {
    @Nonnull
    AzureTaskManager getTaskManager();
}
