/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common.prompt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class InputValidationResultTest {
    @Test
    public void testWrap() {
        final Object obj = new Object();
        final InputValidationResult<Object> wrapper = InputValidationResult.wrap(obj);
        assertNotNull(wrapper);
        assertSame(obj, wrapper.getObj());
        assertNull(wrapper.getErrorMessage());
    }

    @Test
    public void testError() {
        final InputValidationResult<Object> wrapper = InputValidationResult.error("message");
        assertNotNull(wrapper);
        assertEquals("message", wrapper.getErrorMessage());
        assertNull(wrapper.getObj());
    }
}
