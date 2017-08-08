/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctionConfigurationTest {
    @Test
    public void testGetterAndSetter() throws Exception {
        final FunctionConfiguration functionConfiguration = new FunctionConfiguration();

        // setter
        functionConfiguration.setScriptFile("..\\test.jar");
        functionConfiguration.setDisabled(true);
        functionConfiguration.setExcluded(true);
        functionConfiguration.setEntryPoint("foo.bar.function");

        // getter
        assertEquals("..\\test.jar", functionConfiguration.getScriptFile());
        assertTrue(functionConfiguration.isDisabled());
        assertTrue(functionConfiguration.isExcluded());
        assertEquals("foo.bar.function", functionConfiguration.getEntryPoint());
    }
}
