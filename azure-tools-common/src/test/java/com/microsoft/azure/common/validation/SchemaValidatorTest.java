/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.microsoft.azure.common.utils.SneakyThrowUtils;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaValidatorTest {

    private SchemaValidator validator;

    @Before
    public void setup() throws IOException {
        validator = new SchemaValidator();

        final ObjectNode appSchema = (ObjectNode) JsonLoader.fromResource("/testApp.json");
        final ObjectNode appPropertiesNode = (ObjectNode) appSchema.get("properties");
        IteratorUtils.forEach(appPropertiesNode.fields(), prop -> {
            try {
                this.validator.collectSingleProperty("App", prop.getKey(), prop.getValue());
            } catch (JsonProcessingException e) {
                SneakyThrowUtils.sneakyThrow(e);
            }
        });

        final ObjectNode deploymentSchema = (ObjectNode) JsonLoader.fromResource("/testDeployment.json");
        final ObjectNode deploymentPropertiesNode = (ObjectNode) deploymentSchema.get("properties");
        IteratorUtils.forEach(deploymentPropertiesNode.fields(), prop -> {
            try {
                this.validator.collectSingleProperty("Deployment", prop.getKey(), prop.getValue());
            } catch (JsonProcessingException e) {
                SneakyThrowUtils.sneakyThrow(e);
            }
        });

    }

    @Test
    public void testDuplicateAdd() throws IOException {
        try {
            this.validator.collectSingleProperty("Deployment", "cpu", Mockito.mock(JsonNode.class));
            fail("should throw IAE");
        } catch (IllegalArgumentException ex) {
            //expected
        }
    }

    @Test
    public void testValidateSchema() throws Exception {
        validator.getSchemaMap("App", "appName");
        validator.getSchemaMap("Deployment", "deploymentName");
        String error = validator.validateSingleProperty("App", "appName", "foo");
        assertNull(error);
        error = validator.validateSingleProperty("App", "appName", "foo~");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSingleProperty("App", "appName", "foofoofoofoofoofoofoofoofoofoofoo");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSingleProperty("App", "isPublic", "true");
        assertNull(error);

        error = validator.validateSingleProperty("App", "isPublic", "false");
        assertNull(error);

        error = validator.validateSingleProperty("App", "isPublic", "False");
        assertNull(error);

        error = validator.validateSingleProperty("App", "isPublic", "foo");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSingleProperty("Deployment", "cpu", "1");
        assertNull(error);

        error = validator.validateSingleProperty("Deployment", "cpu", "foo");
        System.out.println(error);
        assertNotNull(error);

        error = validator.validateSingleProperty("Deployment", "memoryInGB", "10");
        System.out.println(error);
        assertNotNull(error);
    }

    @Test
    public void testTypeNotSupported() throws Exception {
        final String err = validator.validateSingleProperty("Deployment", "testProperties", "foo");
        assertTrue(err.contains("Type 'array' is not supported in schema validation."));
    }

    @Test
    public void testMoreThanOneViolations() throws Exception {
        final String err = validator.validateSingleProperty("App", "appName", "_thisisaverylonglonglonglonglongtext");
        assertTrue(err.contains("The input violates the validation rules"));
    }

    @Test
    public void testException() throws Exception {

        validator.getSchemaMap("App", "appName");
        validator.getSchemaMap("Deployment", "deploymentName");
        try {
            validator.validateSingleProperty("App", "foo", "foo");
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            validator.validateSingleProperty("Foo", "foo", "foo");
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
