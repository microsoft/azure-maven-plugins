/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.maven.appservice.DeploymentTypeValues;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.artifacthandler.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.artifacthandler.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.handlers.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.NONEArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.WarArtifactHandlerImpl;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

/**
 * Types of deployments supported by this Maven plugin.
 *
 * @since 0.1
 */
public enum DeploymentType {
    NONE(new NONEHandler()),
    FTP(new FTPHandler()),
    ZIP(new ZIPHandler()),
    WAR(new WARHandler()),
    JAR(new JARHandler()),
    AUTO(new AUTOHandler());

    private Handler handler;

    public static final String UNKNOWN_DEPLOYMENT_TYPE = String.format(
            "The value of <deploymentType> is unknown, supported values are: %s, %s, %s, %s and %s.",
            DeploymentTypeValues.JAR,
            DeploymentTypeValues.WAR,
            DeploymentTypeValues.ZIP,
            DeploymentTypeValues.FTP,
            DeploymentTypeValues.AUTO
    );

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
    public ArtifactHandler getArtifactHandlerFromMojo(AbstractWebAppMojo mojo) throws MojoExecutionException {
        return handler.apply(mojo);
    }

    public static DeploymentType fromString(final String input) throws MojoExecutionException {
        if (StringUtils.isEmpty(input)) {
            throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case DeploymentTypeValues.FTP:
                return FTP;
            case DeploymentTypeValues.ZIP:
                return ZIP;
            case DeploymentTypeValues.WAR:
                return WAR;
            case DeploymentTypeValues.JAR:
                return JAR;
            case DeploymentTypeValues.NONE:
                return NONE;
            case DeploymentTypeValues.AUTO:
                return AUTO;
            default:
                throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
    }

    // TODO: Change to lambda once on Java 8+
    interface Handler {
        ArtifactHandler apply(AbstractWebAppMojo m) throws MojoExecutionException;
    }

    static class NONEHandler implements Handler {
        public ArtifactHandler apply(AbstractWebAppMojo m)  {
            return new NONEArtifactHandlerImpl(m);
        }
    }

    static class AUTOHandler implements Handler {
        public ArtifactHandler apply(AbstractWebAppMojo m) throws MojoExecutionException {
            String packaging = m.getProject().getPackaging();
            packaging = packaging != null ? packaging.toLowerCase(Locale.ENGLISH).trim() : "";
            switch (packaging) {
                case "war":
                    return new WarArtifactHandlerImpl(m);
                case "jar":
                    return new JarArtifactHandlerImpl(m);
                default:
                    throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
            }
        }
    }

    static class FTPHandler implements Handler {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new FTPArtifactHandlerImpl(m);
        }
    }

    static class ZIPHandler implements Handler {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new ZIPArtifactHandlerImpl(m);
        }
    }

    static class WARHandler implements Handler {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new WarArtifactHandlerImpl(m);
        }
    }

    static class JARHandler implements Handler {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new JarArtifactHandlerImpl(m);
        }
    }
}
