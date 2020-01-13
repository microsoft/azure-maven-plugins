/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.maven.function;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.function.template.BindingTemplate;
import com.microsoft.azure.maven.function.template.BindingsTemplate;
import com.microsoft.azure.maven.function.template.FunctionTemplate;
import com.microsoft.azure.maven.function.template.FunctionTemplates;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FunctionUtils {
    private static final String LOAD_TEMPLATES_FAIL = "Failed to load all function templates.";
    private static final String LOAD_BINDING_TEMPLATES_FAIL = "Failed to load function binding template.";

    public static BindingTemplate loadBindingTemplate(String type) {
        try (final InputStream is = Constants.class.getResourceAsStream("/bindings.json")) {
            final String bindingsJsonStr = IOUtils.toString(is, "utf8");
            final BindingsTemplate bindingsTemplate = new ObjectMapper()
                .readValue(bindingsJsonStr, BindingsTemplate.class);
            return bindingsTemplate.getBindingTemplateByName(type);
        } catch (IOException e) {
            Log.warn(LOAD_BINDING_TEMPLATES_FAIL);
            // Add task should work without Binding Template, just return null if binding load fail
            return null;
        }
    }

    public static List<FunctionTemplate> loadAllFunctionTemplates() throws AzureExecutionException {
        try (final InputStream is = FunctionUtils.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtils.toString(is, "utf8");
            final List<FunctionTemplate> templates = parseTemplateJson(templatesJsonStr);
            return templates;
        } catch (Exception e) {
            Log.error(LOAD_TEMPLATES_FAIL);
            throw new AzureExecutionException(LOAD_TEMPLATES_FAIL, e);
        }
    }

    private static List<FunctionTemplate> parseTemplateJson(final String templateJson) throws JsonParseException, JsonMappingException, IOException {
        final FunctionTemplates templates = new ObjectMapper().readValue(templateJson, FunctionTemplates.class);
        return templates.getTemplates();
    }
}
