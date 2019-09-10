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

public class InputValidateResultTest {
    @Test
    public void testWrap() {
        final Object obj = new Object();
        final InputValidateResult<Object> wrapper = InputValidateResult.wrap(obj);
        assertNotNull(wrapper);
        assertSame(obj, wrapper.getObj());
        assertNull(wrapper.getErrorMessage());
    }

    @Test
    public void testError() {
        final InputValidateResult<Object> wrapper = InputValidateResult.error("message");
        assertNotNull(wrapper);
        assertEquals("message", wrapper.getErrorMessage());
        assertNull(wrapper.getObj());
    }
}
