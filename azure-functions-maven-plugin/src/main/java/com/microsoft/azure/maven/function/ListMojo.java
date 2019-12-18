/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.Mojo;

import com.microsoft.azure.common.function.ListHandler;

/**
 * The list mojo is used to provide Java Azure Functions templates information,
 * which is used by other tools such as VS Code Azure Functions extension.
 */
@Mojo(name = "list")
public class ListMojo extends AbstractFunctionMojo {

    @Override
    protected void doExecute() throws Exception {
    	new ListHandler().execute();
    }

    protected void printToSystemOut(String file) throws IOException {
        try (final InputStream is = ListMojo.class.getResourceAsStream(file)) {
            IOUtils.copy(is, System.out);
        }
    }
}
