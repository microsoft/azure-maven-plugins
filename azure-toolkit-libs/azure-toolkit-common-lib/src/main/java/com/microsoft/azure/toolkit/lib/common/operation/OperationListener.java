/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

public interface OperationListener {
    default void beforeEnter(Operation operation, Object source) {
    }

    default void afterReturning(Operation operation, Object source) {
    }

    default void afterThrowing(Throwable e, Operation operation, Object source) {
    }
}
