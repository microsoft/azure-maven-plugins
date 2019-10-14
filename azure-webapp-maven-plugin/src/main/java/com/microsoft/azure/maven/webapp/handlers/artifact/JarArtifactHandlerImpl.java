/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.google.common.io.Files;
import com.microsoft.azure.maven.artifacthandler.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.DEFAULT_APP_SERVICE_JAR_NAME;

/**
 * Artifact handler for deploying a JAR, self-contained, Java application (e.g.
 * Spring Boot) to Azure App Service through FTP
 *
 * @since 1.3.0
 */
public final class JarArtifactHandlerImpl extends ZIPArtifactHandlerImpl {
    private String linuxRuntime;
    private String jarFile;

    public static final String FILE_IS_NOT_JAR = "The deployment file is not a jar typed file.";
    public static final String FIND_JAR_FILE_FAIL = "Failed to find the jar file: '%s'";

    public static class Builder extends ZIPArtifactHandlerImpl.Builder {
        private String jarFile;
        private String linuxRuntime;

        @Override
        protected JarArtifactHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public JarArtifactHandlerImpl build() {
            return new JarArtifactHandlerImpl(this);
        }

        public Builder linuxRuntime(final String value) {
            this.linuxRuntime = value;
            return self();
        }

        public Builder jarFile(final String value) {
            this.jarFile = value;
            return self();
        }
    }

    protected JarArtifactHandlerImpl(final Builder builder) {
        super(builder);
        this.linuxRuntime = builder.linuxRuntime;
        this.jarFile = builder.jarFile;
    }

    /**
     * Jar deploy prepares deployment file itself.
     * So preparing resources to staging folder is not necessary.
     */
    @Override
    protected boolean isResourcesPreparationRequired(final DeployTarget target) {
        return false;
    }

    @Override
    public void publish(DeployTarget deployTarget) throws IOException, MojoExecutionException {
        final File jar = getJarFile();
        assureJarFileExisted(jar);

        prepareDeploymentFiles(jar);

        super.publish(deployTarget);
    }

    protected void prepareDeploymentFiles(File jar) throws IOException {
        final File parent = new File(stagingDirectoryPath);
        parent.mkdirs();
        Files.copy(jar, new File(parent, DEFAULT_APP_SERVICE_JAR_NAME));
    }

    protected File getJarFile() {
        final String jarFilePath = StringUtils.isNotEmpty(jarFile) ? jarFile :
                Paths.get(buildDirectoryAbsolutePath, project.getBuild().getFinalName() + ".jar").toString();
        return new File(jarFilePath);
    }

    protected void assureJarFileExisted(File jar) throws MojoExecutionException {
        if (!Files.getFileExtension(jar.getName()).equalsIgnoreCase("jar")) {
            throw new MojoExecutionException(FILE_IS_NOT_JAR);
        }

        if (!jar.exists() || !jar.isFile()) {
            throw new MojoExecutionException(String.format(FIND_JAR_FILE_FAIL, jar.getAbsolutePath()));
        }
    }

}
