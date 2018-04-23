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
import org.codehaus.plexus.util.StringUtils;

import com.google.common.io.Files;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

public class WarArtifactHandlerImpl implements ArtifactHandler  {

    public static final String FILE_IS_NOT_WAR = "The deployment file is not a war typed file.";
    private AbstractWebAppMojo mojo;

    public static final String FIND_WAR_FILE_FAIL = "Failed to find the war file: '%s'";
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;
    public static final String UPLOAD_FAILURE = "Failed to deploy the war file to server, " +
            "retrying immediately (%d/%d)";

    public WarArtifactHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish(final List<Resource> resources) throws Exception {
        final File war = getWarFile();

        assureWarFileExisted(war);

        mojo.getLog().info("Starting to deploy the war file...");

        final String path = getContextPath();

        final WebApp app = mojo.getWebApp();
        int retryCount = 0;
        while (retryCount++ < DEFAULT_MAX_RETRY_TIMES) {
            try {
                app.warDeploy(war, path);
                return;
            } catch (Exception e) {
                mojo.getLog().warn(String.format(UPLOAD_FAILURE, retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

    }

    protected String getContextPath() {
        String path = mojo.getPath().trim();
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

    protected File getWarFile() {
        return StringUtils.isNotEmpty(mojo.getWarFile()) ? new File(mojo.getWarFile()) :
                    new File(Paths.get(mojo.getBuildDirectoryAbsolutePath(),
                            mojo.getProject().getBuild().getFinalName() + ".war").toString());
    }

    protected void assureWarFileExisted(File war) throws MojoExecutionException {
        if (!Files.getFileExtension(war.getName()).equalsIgnoreCase("war")) {
            throw new MojoExecutionException(FILE_IS_NOT_WAR);
        }

        if (!war.exists() || !war.isFile()) {
            throw new MojoExecutionException(String.format(FIND_WAR_FILE_FAIL, war.getAbsolutePath()));
        }
    }
}
