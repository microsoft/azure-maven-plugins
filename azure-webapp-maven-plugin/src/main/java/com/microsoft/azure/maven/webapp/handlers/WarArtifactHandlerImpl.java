/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.google.common.io.Files;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.deployadapter.IDeployTargetAdapter;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.nio.file.Paths;

public class WarArtifactHandlerImpl implements ArtifactHandler  {

    public static final String FILE_IS_NOT_WAR = "The deployment file is not a war typed file.";
    public static final String FIND_WAR_FILE_FAIL = "Failed to find the war file: '%s'";
    public static final String UPLOAD_FAILURE = "Exception occurred when deploying war file to server: %s, " +
        "retrying immediately (%d/%d)";
    public static final String DEPLOY_FAILURE = "Failed to deploy the war file after three times trying.";
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;

    private AbstractWebAppMojo mojo;

    public WarArtifactHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish(IDeployTargetAdapter deployTarget) throws MojoExecutionException {
        final File war = getWarFile();

        assureWarFileExisted(war);

        final String path = getContextPath();

        int retryCount = 0;
        mojo.getLog().info("Deploying the war file...");
        while (retryCount++ < DEFAULT_MAX_RETRY_TIMES) {
            try {
                deployTarget.warDeploy(war, path);
                return;
            } catch (Exception e) {
                mojo.getLog().warn(String.format(UPLOAD_FAILURE, e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        if (retryCount > DEFAULT_MAX_RETRY_TIMES) {
            throw new MojoExecutionException(DEPLOY_FAILURE);
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
