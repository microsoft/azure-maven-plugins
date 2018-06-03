/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.io.Files;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Artifact handler for deploying a JAR, self-contained, Java application (e.g.
 * Spring Boot) to Azure App Service through FTP
 * 
 * @since 1.2.0
 */
public final class JarArtifactHandlerImpl extends FTPArtifactHandlerImpl {

    public static final String FILE_IS_NOT_JAR = "The deployment file is not a jar typed file.";
    public static final String FIND_JAR_FILE_FAIL = "Failed to find the jar file: '%s'";

    private static final String JAR_CMD = "__JAR_COMMAND__";
    private static final String DEFAULT_JAR_COMMAND = "-Djava.net.preferIPv4Stack=true "
            + "-Dserver.port=%HTTP_PLATFORM_PORT% "
            + "-jar &quot;%HOME%\\\\site\\\\wwwroot\\\\FILENAME&quot;";

    public JarArtifactHandlerImpl(final AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public void publish() throws Exception {
        // Ensure stage directory exists
        final File parent = new File(mojo.getDeploymentStageDirectory());
        parent.mkdirs();

        // Copy JAR file to stage dir
        final File jar = getJarFile();
        assureJarFileExisted(jar);
        Files.copy(jar, new File(parent, jar.getName()));

        // Generate web.config file
        generateWebConfigFile(jar.getName());

        // Copy any other resource
        final List<Resource> resources = mojo.getResources();
        copyResourcesToStageDirectory(resources);

        // FTP Upload all to wwwroot
        uploadDirectoryToFTP();
    }

    private String getJarCommand() {
        return mojo.getJarCommand() == null ? DEFAULT_JAR_COMMAND : mojo.getJarCommand();
    }

    private void generateWebConfigFile(String jarFileName) {
        try {
            final InputStream is = getClass().getResourceAsStream("web.config.template");
            final String templateContent = IOUtils.toString(is, "UTF-8");
            is.close();

            final String webConfigFile = templateContent
                                .replaceAll(JAR_CMD, getJarCommand())
                                .replaceAll("FILENAME", jarFileName);

            final File webconfig = new File(mojo.getDeploymentStageDirectory(), "web.config");
            webconfig.createNewFile();

            final FileOutputStream fos = new FileOutputStream(webconfig);
            IOUtils.write(webConfigFile, fos, "UTF-8");
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e, "It was not possible to generate web.config file for JAR deployment");
        }
    }

    protected File getJarFile() {
        return StringUtils.isNotEmpty(mojo.getJarFile()) ? new File(mojo.getJarFile())
                : new File(Paths
                        .get(mojo.getBuildDirectoryAbsolutePath(), mojo.getProject().getBuild().getFinalName() + ".jar")
                        .toString());
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
