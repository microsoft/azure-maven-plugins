/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.handlers.*;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public abstract class DeployFacadeBaseImpl implements DeployFacade {
    public static final String NO_RESOURCES_CONFIG = "No resources specified in pom.xml. Skip artifacts deployment.";

    private AbstractWebAppMojo mojo;

    public DeployFacadeBaseImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public abstract DeployFacade setupRuntime() throws MojoExecutionException;

    public abstract DeployFacade applySettings() throws MojoExecutionException;

    public abstract DeployFacade commitChanges() throws MojoExecutionException;

    public DeployFacade deployArtifacts() throws Exception {
        final List<Resource> resources = getMojo().getResources();
        if (resources == null || resources.isEmpty()) {
            getMojo().getLog().warn(NO_RESOURCES_CONFIG);
        } else {
            getArtifactHandler().publish(resources);
        }
        return this;
    }

    protected AbstractWebAppMojo getMojo() {
        return mojo;
    }

    protected RuntimeHandler getRuntimeHandler() throws MojoExecutionException {
        final JavaVersion javaVersion = getMojo().getJavaVersion();
        final ContainerSetting containerSetting = getMojo().getContainerSettings();
        if (javaVersion != null && containerSetting != null && !containerSetting.isEmpty()) {
            throw new MojoExecutionException("<javaVersion> and <containerSettings> are mutual exclusive.");
        }

        if (javaVersion != null) {
            return new JavaRuntimeHandlerImpl(mojo);
        }

        if (StringUtils.isEmpty(containerSetting.getServerId())) {
            return new PublicDockerHubRuntimeHandlerImpl(getMojo());
        }

        if (StringUtils.isEmpty(containerSetting.getRegistryUrl())) {
            return new PrivateDockerHubRuntimeHandlerImpl(getMojo());
        }

        return new PrivateRegistryRuntimeHandlerImpl(getMojo());
    }

    protected SettingsHandler getSettingsHandler() {
        return new SettingsHandlerImpl(getMojo());
    }

    private ArtifactHandler getArtifactHandler() throws MojoExecutionException {
        switch (getMojo().getDeploymentType()) {
            case LOCAL_GIT:
                throw new NotImplementedException();
            case FTP:
            default:
                return new FTPArtifactHandlerImpl(mojo);
        }
    }
}
