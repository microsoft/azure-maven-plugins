/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.handlers.ArtifactHandler;
import com.microsoft.azure.maven.webapp.handlers.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.WarArtifactHandlerImpl;
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
    WAR(new WARHandler()),
    JAR(new JARHandler()),
    UNKNOWN(new UNKNOWNHandler());

    private Function getHandler;

    DeploymentType(Function<AbstractWebAppMojo, ArtifactHandler> getHandler) {
        this.getHandler = getHandler;
    }

    /**
     * Identifies the proper artifact handler based on the type of the deployment configured in the Maven Mojo object.
     * It will inspect first the <deploymentType> property. If NONE, it will look into <packaging> of the Maven
     * artifact.
     *
     * @param mojo for the Maven project
     * @return an ArtifactHandler mapped to the deployment type identified
     */
    public ArtifactHandler getArtifactHandlerFromMojo(AbstractWebAppMojo mojo) {
        return getHandler.apply(mojo);
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
            default:
                return UNKNOWN;
        }
    }

    // TODO: Change to lambda once on Java 8+
    interface Function<T, R> {
        ArtifactHandler apply(AbstractWebAppMojo m);
    }

    static class NONEHandler implements Function {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            switch (m.getProject().getPackaging()) {
                case "war":
                    return new WarArtifactHandlerImpl(m);
                case "jar":
                    return new JarArtifactHandlerImpl(m);
                default:
                    throw new RuntimeException("You must set a packaging type of (jar, war) or a " +
                            "deployment type in the Maven plugin configuration.");
            }
        }
    }

    static class FTPHandler implements Function {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new FTPArtifactHandlerImpl(m);
        }
    }

    static class WARHandler implements Function {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new WarArtifactHandlerImpl(m);
        }
    }

    static class JARHandler implements Function {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            return new JarArtifactHandlerImpl(m);
        }
    }

    static class UNKNOWNHandler implements Function {
        public ArtifactHandler apply(AbstractWebAppMojo m) {
            throw new RuntimeException("Unknown deployment type.");
        }
    }
}
