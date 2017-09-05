/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.webapp.handlers.ArtifactHandler;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.SettingsHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.mockito.Mock;

import java.lang.reflect.Field;

public abstract class DeployFacadeTestBase {
    @Mock
    protected AbstractWebAppMojo mojo;

    @Mock
    protected Log log;

    @Mock
    protected ArtifactHandler artifactHandler;

    @Mock
    protected RuntimeHandler runtimeHandler;

    @Mock
    protected SettingsHandler settingsHandler;

    protected void setupHandlerFactory() throws Exception {
        final Field f = HandlerFactory.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, new HandlerFactory() {
            @Override
            public RuntimeHandler getRuntimeHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return runtimeHandler;
            }

            @Override
            public SettingsHandler getSettingsHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return settingsHandler;
            }

            @Override
            public ArtifactHandler getArtifactHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return artifactHandler;
            }
        });
    }
}
