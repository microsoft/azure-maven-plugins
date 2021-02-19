/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.utils;

import org.junit.Test;

import java.io.IOException;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;

public class SneakyThrowUtilsTest {
    @Test
    public void testSneakyThrowLambda() {
        final Supplier<Integer> throwIOException = () -> {
            try {
                return throwIOException();
            } catch (IOException e) {
                return SneakyThrowUtils.sneakyThrow(e);
            }
        };
        try {
            throwIOException.get();
        } catch (Exception ex) {
            assertTrue(ex instanceof IOException);
        }
    }

    private static Integer throwIOException() throws IOException {
        throw new IOException();
    }
}
