/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.io.InputStream;

/**
 * The list mojo is used to provide Java Azure Functions templates information,
 * which is used by other tools such as VS Code Azure Functions extension.
 */
@Mojo(name = "list")
public class ListMojo extends AbstractFunctionMojo {

    protected static final String TEMPLATES_START = ">> templates begin <<";
    protected static final String TEMPLATES_END = ">> templates end <<";

    protected static final String BINDINGS_START = ">> bindings begin <<";
    protected static final String BINDINGS_END = ">> bindings end <<";

    protected static final String RESOURCES_START = ">> resources begin <<";
    protected static final String RESOURCES_END = ">> resources end <<";

    protected static final String TEMPLATES_FILE = "/templates.json";
    protected static final String BINDINGS_FILE = "/bindings.json";
    protected static final String RESOURCES_FILE = "/resources.json";

    @Override
    protected void doExecute() throws AzureExecutionException {
        try {
            Log.info(TEMPLATES_START);
            printToSystemOut(TEMPLATES_FILE);
            Log.info(TEMPLATES_END);

            Log.info(BINDINGS_START);
            printToSystemOut(BINDINGS_FILE);
            Log.info(BINDINGS_END);

            Log.info(RESOURCES_START);
            printToSystemOut(RESOURCES_FILE);
            Log.info(RESOURCES_END);
        } catch (IOException e) {
            throw new AzureExecutionException("IO errror when printing templates:" + e.getMessage(), e);
        }
    }

    protected void printToSystemOut(String file) throws IOException {
        try (final InputStream is = ListMojo.class.getResourceAsStream(file)) {
            IOUtils.copy(is, System.out);
        }
    }
}
