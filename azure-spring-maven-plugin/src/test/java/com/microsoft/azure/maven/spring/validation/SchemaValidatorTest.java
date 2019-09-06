/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.validation;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SchemaValidatorTest {
    private final SchemaValidator validator = new SchemaValidator();

    @Test
    public void testValidateSchema() throws Exception {
        validator.getSchemaMap("App", "appName");
        validator.getSchemaMap("Deployment", "deploymentName");
        String error = validator.validateSchema("App", "appName", "foo");
        assertNull(error);
        error = validator.validateSchema("App", "appName", "foo~");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSchema("App", "appName", "foofoofoofoofoofoofoofoofoofoofoo");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSchema("App", "isPublic", "true");
        assertNull(error);

        error = validator.validateSchema("App", "isPublic", "false");
        assertNull(error);

        error = validator.validateSchema("App", "isPublic", "False");
        assertNull(error);

        error = validator.validateSchema("App", "isPublic", "foo");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSchema("Deployment", "cpu", "1");
        assertNull(error);

        error = validator.validateSchema("Deployment", "cpu", "foo");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSchema("Deployment", "memoryInGB", "10");
        System.out.println(error);
        assertNotNull(error);
    }

    @Test
    public void testException() throws Exception {

        validator.getSchemaMap("App", "appName");
        validator.getSchemaMap("Deployment", "deploymentName");
        try {
            validator.validateSchema("App", "foo", "foo");
            fail("Shoudl throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            validator.validateSchema("Foo", "foo", "foo");
            fail("Shoudl throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
