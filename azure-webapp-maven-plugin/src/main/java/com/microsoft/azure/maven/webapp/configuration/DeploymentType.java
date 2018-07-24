/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.maven.artifacthandler.FTPArtifactHandler;
import com.microsoft.azure.maven.artifacthandler.IArtifactHandler;
import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.handlers.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.WarArtifactHandlerImpl;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

/**
 * Types of deployments supported by this Maven plugin.
 *
 * @since 0.1
 */
public enum DeploymentType {
    NONE(new NONEHandler()),
    FTP(new FTPHandler()),
    WAR(new WARHandler()),
    JAR(new JARHandler()),
    AUTO(new AUTOHandler()),
    UNKNOWN(new UNKNOWNHandler());

    private Handler handler;

    DeploymentType(Handler handler) {
        this.handler = handler;
    }

    /**
     * Identifies the proper artifact handler based on the type of the deployment configured in the Maven Mojo object.
     * It will inspect first the <deploymentType> property. If NONE, it will look into <packaging> of the Maven
     * artifact.
     *
     * @param mojo for the Maven project
     * @return an ArtifactHandler mapped to the deployment type identified
     */
    public IArtifactHandler getArtifactHandlerFromMojo(AbstractWebAppMojo mojo) throws MojoExecutionException {
        return handler.apply(mojo);
    }

    public static DeploymentType fromString(final String input) {
        if (StringUtils.isEmpty(input)) {
            return NONE;
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "FTP":
                return FTP;
            case "WAR":
                return WAR;
            case "JAR":
                return JAR;
            case "NONE":
                return NONE;
            case "AUTO":
                return AUTO;
            default:
                return UNKNOWN;
        }
    }

    // TODO: Change to lambda once on Java 8+
    interface Handler {
        IArtifactHandler apply(AbstractWebAppMojo m) throws MojoExecutionException;
    }

    private static class NONEArtifactHandlerImplementation implements IArtifactHandler {
        @Override
        public void publish(BaseDeployTarget deployTarget) {
            // does nothing
        }
    }

    static class NONEHandler implements Handler {
        public IArtifactHandler apply(AbstractWebAppMojo m)  {
            return new NONEArtifactHandlerImplementation();
        }
    }

    static class AUTOHandler implements Handler {
        public IArtifactHandler apply(AbstractWebAppMojo m) {
            String packaging = m.getProject().getPackaging();
            packaging = packaging != null ? packaging.toLowerCase(Locale.ENGLISH) : "";
            switch (packaging.trim()) {
                case "war":
                    return new WarArtifactHandlerImpl(m);
                case "jar":
                    return new JarArtifactHandlerImpl(m, m.getResources(),
                        m.getJarFile(), StringUtils.isNotEmpty(m.getLinuxRuntime()));
                default:
                    return new NONEArtifactHandlerImplementation();
            }
        }
    }

    static class FTPHandler implements Handler {
        public IArtifactHandler apply(AbstractWebAppMojo m) {
            return new FTPArtifactHandler(m, m.getResources());
        }
    }

    static class WARHandler implements Handler {
        public IArtifactHandler apply(AbstractWebAppMojo m) {
            return new WarArtifactHandlerImpl(m);
        }
    }

    static class JARHandler implements Handler {
        public IArtifactHandler apply(AbstractWebAppMojo m) {
            return new JarArtifactHandlerImpl(m, m.getResources(),
                m.getJarFile(), StringUtils.isNotEmpty(m.getLinuxRuntime()));
        }
    }

    static class UNKNOWNHandler implements Handler {
        public IArtifactHandler apply(AbstractWebAppMojo m) {
            throw new RuntimeException("Unknown deployment type.");
        }
    }
}
