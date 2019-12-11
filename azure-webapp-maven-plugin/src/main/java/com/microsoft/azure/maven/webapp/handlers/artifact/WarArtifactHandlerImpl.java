/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.google.common.io.Files;
import com.microsoft.azure.maven.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.io.File;
import java.nio.file.Paths;

/**
 * Artifact handler used to deploy war file. Typically, each artifact handler needs to extends
 * {@link ArtifactHandlerBase}
 * WarArtifactHandler is a special case because it does not need staging folder.
 * Thus, the methods shared in ArtifactHandlerBase is not needed here.
 */
public class WarArtifactHandlerImpl extends ArtifactHandlerBase {
    protected final String warFile;
    protected final String contextPath;

    public static final String FILE_IS_NOT_WAR = "The deployment file is not a war typed file.";
    public static final String FIND_WAR_FILE_FAIL = "Failed to find the war file: '%s'";
    public static final String UPLOAD_FAILURE = "Exception occurred when deploying war file to server: %s, " +
        "retrying immediately (%d/%d)";
    public static final String DEPLOY_FAILURE = "Failed to deploy war file after %d times of retry.";
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;

    public static class Builder extends ArtifactHandlerBase.Builder<WarArtifactHandlerImpl.Builder> {
        private String warFile;
        private String contextPath;

        @Override
        protected WarArtifactHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public WarArtifactHandlerImpl build() {
            return new WarArtifactHandlerImpl(this);
        }

        public Builder warFile(final String value) {
            this.warFile = value;
            return self();
        }

        public Builder contextPath(final String value) {
            this.contextPath = value;
            return self();
        }
    }

    protected WarArtifactHandlerImpl(final WarArtifactHandlerImpl.Builder builder) {
        super(builder);
        this.contextPath = builder.contextPath;
        this.warFile = builder.warFile;
    }

    @Override
    public void publish(final DeployTarget target) throws MojoExecutionException {

        final File war = getWarFile();

        assureWarFileExisted(war);

        final Runnable warDeployExecutor = ArtifactHandlerUtils.getRealWarDeployExecutor(target, war, getContextPath());
        log.info(String.format(DEPLOY_START, target.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        int retryCount = 0;
        log.info("Deploying the war file...");

        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount++;
            try {
                warDeployExecutor.run();
                log.info(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
                return;
            } catch (Exception e) {
                log.debug(String.format(UPLOAD_FAILURE, e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        throw new MojoExecutionException(String.format(DEPLOY_FAILURE, DEFAULT_MAX_RETRY_TIMES));
    }

    protected String getContextPath() {
        String path = contextPath.trim();
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

    protected File getWarFile() {
        return StringUtils.isNotEmpty(warFile) ? new File(warFile) :
            new File(Paths.get(buildDirectoryAbsolutePath, project.getBuild().getFinalName() + ".war").toString());
    }

    protected void assureWarFileExisted(final File war) throws MojoExecutionException {
        if (!Files.getFileExtension(war.getName()).equalsIgnoreCase("war")) {
            throw new MojoExecutionException(FILE_IS_NOT_WAR);
        }

        if (!war.exists() || !war.isFile()) {
            throw new MojoExecutionException(String.format(FIND_WAR_FILE_FAIL, war.getAbsolutePath()));
        }
    }
}
