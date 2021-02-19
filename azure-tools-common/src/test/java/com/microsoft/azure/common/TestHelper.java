/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.stream.Collectors;

public class TestHelper {
    public static String joinIntegers(List<Integer> integers) {
        Preconditions.checkNotNull(integers, "Parameter 'integers' should not be null or empty.");
        return integers.stream().map(t -> t.toString()).collect(Collectors.joining(","));
    }
}
