/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import org.apache.maven.plugin.MojoExecutionException;

public interface Handler {
    Handler setupRuntime() throws MojoExecutionException;

    Handler applySettings() throws Exception;

    Handler deployArtifacts() throws Exception;

    Handler commit() throws Exception;
}
