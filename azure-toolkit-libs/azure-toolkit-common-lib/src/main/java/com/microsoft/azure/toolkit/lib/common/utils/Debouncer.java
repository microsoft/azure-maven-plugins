/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

public interface Debouncer {
    void debounce();

    void debounce(int delay);

    boolean isPending();
}
