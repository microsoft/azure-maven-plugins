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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public abstract class DeployFacadeBaseImpl implements DeployFacade {
    public static final String NO_RESOURCES_CONFIG = "No resources specified in pom.xml. Skip artifacts deployment.";
    public static final String RUNTIME_CONFIG_CONFLICT = "<javaVersion> is for Web App on Windows; " +
            "<containerSettings> is for Web App on Linux; they can't be specified at the same time.";
    public static final String NO_RUNTIME_HANDLER = "Not able to process the runtime stack configuration; " +
            "please check <javaVersion> or <containerSettings> tag.";

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

        // Neither <javaVersion> nor <containerSettings> is specified
        if (javaVersion == null && (containerSetting == null || containerSetting.isEmpty())) {
            return new NullRuntimeHandlerImpl(getMojo());
        }

        // Both <javaVersion> and <containerSettings> are specified
        if (javaVersion != null && containerSetting != null && !containerSetting.isEmpty()) {
            throw new MojoExecutionException(RUNTIME_CONFIG_CONFLICT);
        }

        if (javaVersion != null) {
            return new JavaRuntimeHandlerImpl(getMojo());
        }

        if (WebAppUtils.isPublicDockerHubImage(containerSetting)) {
            return new PublicDockerHubRuntimeHandlerImpl(getMojo());
        }

        if (WebAppUtils.isPrivateDockerHubImage(containerSetting)) {
            return new PrivateDockerHubRuntimeHandlerImpl(getMojo());
        }

        if (WebAppUtils.isPrivateRegistryImage(containerSetting)) {
            return new PrivateRegistryRuntimeHandlerImpl(getMojo());
        }

        throw new MojoExecutionException(NO_RUNTIME_HANDLER);
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
                return new FTPArtifactHandlerImpl(getMojo());
        }
    }
}
