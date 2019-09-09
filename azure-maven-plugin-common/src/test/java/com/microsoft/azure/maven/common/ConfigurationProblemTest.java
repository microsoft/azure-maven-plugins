/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common;

import com.microsoft.azure.maven.common.ConfigurationProblem.Severity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigurationProblemTest {
    @Test
    public void testCtor() {
        final ConfigurationProblem problem = new ConfigurationProblem("key", "value", "error", Severity.ERROR);
        assertNotNull(problem);
        assertEquals("key", problem.getKey());
        assertEquals("value", problem.getValue());
        assertEquals("error", problem.getErrorMessage());
        assertEquals("ERROR", problem.getSeverity().toString());
    }
}
