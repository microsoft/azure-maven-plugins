/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.util;

public class Utils {
    public static <T> T firstNonNull(T... args) {
        for (T obj : args) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }
}
