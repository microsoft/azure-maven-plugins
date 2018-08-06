/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.InputStream;

/**
 * The list mojo is used to provide Java Azure Functions templates information,
 * which is used by other tools such as VS Code Azure Functions extension.
 */
@Mojo(name = "list")
public class ListMojo extends AbstractFunctionMojo {

    protected static final String PRINTING_START = ">> templates begin <<";
    protected static final String PRINT_END = ">> templates end <<";

    @Override
    protected void doExecute() throws Exception {
        info(PRINTING_START);

        try (final InputStream is = ListMojo.class.getResourceAsStream("/templates.json")) {
            IOUtils.copy(is, System.out);
        }

        info(PRINT_END);
    }
}
