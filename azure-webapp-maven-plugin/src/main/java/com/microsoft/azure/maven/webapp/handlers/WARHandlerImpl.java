/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.io.Files;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

public class WARHandlerImpl implements ArtifactHandler  {

    private AbstractWebAppMojo mojo;

    private static final int DEFAULT_MAX_RETRY_TIMES = 5;
    private static final String UPLOAD_FAILURE = "Failed to deploy the war file to server, " +
            "retrying immediately (%d/%d)";

    public WARHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish(final List<Resource> resources) throws Exception {
        final File war = new File(Paths.get(mojo.getBuildDirectoryAbsolutePath(),
                mojo.getProject().getBuild().getFinalName() + "." + mojo.getProject().getPackaging()).toString());
        if (!war.exists() || !war.isFile() || !Files.getFileExtension(war.getName()).equalsIgnoreCase("war")) {
            throw new MojoExecutionException("Failed to find the war file in build directory");
        }
        mojo.getLog().info("Starting to deploy the war file...");
        int retryCount = 0;
        final WebApp app = mojo.getWebApp();
        while (retryCount++ < DEFAULT_MAX_RETRY_TIMES) {
            try {
                app.warDeploy(war);
                return;
            } catch (Exception e) {
                mojo.getLog().warn(String.format(UPLOAD_FAILURE, retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

    }
}
