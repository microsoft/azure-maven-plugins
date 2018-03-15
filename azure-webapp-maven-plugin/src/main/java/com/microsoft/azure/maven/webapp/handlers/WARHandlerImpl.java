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
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

public class WARHandlerImpl implements ArtifactHandler  {

    private AbstractWebAppMojo mojo;

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
        mojo.getWebApp().warDeploy(war);
    }
}
