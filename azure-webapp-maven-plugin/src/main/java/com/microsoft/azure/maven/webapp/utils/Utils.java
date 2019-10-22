/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

public class Utils {

    private static final String CREATE_TEMP_FILE_FAIL = "Failed to create temp file %s.%s";

    public static File createTempFile(final String prefix, final String suffix) throws MojoExecutionException {
        try {
            final File zipFile = File.createTempFile(prefix, suffix);
            zipFile.deleteOnExit();
            return zipFile;
        } catch (IOException e) {
            throw new MojoExecutionException(String.format(CREATE_TEMP_FILE_FAIL, prefix, suffix), e.getCause());
        }
    }

    private Utils(){

    }
}
