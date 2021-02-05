/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

public class Utils {
    public static String getId(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }
}
