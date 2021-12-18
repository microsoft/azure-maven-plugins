/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common;

@FunctionalInterface
public interface Executable<T> {
    T execute() throws Throwable;
}

