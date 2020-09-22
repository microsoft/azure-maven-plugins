/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.handlers.RuntimeHandler;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import com.microsoft.azure.maven.webapp.handlers.artifact.JarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.artifact.NONEArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.handlers.artifact.WarArtifactHandlerImpl;
import com.microsoft.azure.maven.webapp.utils.FTPUtils;
import com.microsoft.azure.maven.webapp.utils.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    private static final Path FTP_ROOT = Paths.get("/site/wwwroot");
    private static final String NO_RESOURCES_CONFIG = "<resources> is empty. Please make sure it is configured in pom.xml.";

    public static final String WEBAPP_NOT_EXIST = "Target Web App doesn't exist. Creating a new one...";
    public static final String WEBAPP_CREATED = "Successfully created Web App.";
    public static final String CREATE_DEPLOYMENT_SLOT = "Target Deployment Slot doesn't exist. Creating a new one...";
    public static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created the Deployment Slot.";
    public static final String UPDATE_WEBAPP = "Updating target Web App...";
    public static final String UPDATE_WEBAPP_SKIP = "No runtime configured. Skip the update.";
    public static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App.";
    public static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    public static final String START_APP = "Starting Web App after deploying artifacts...";
    public static final String STOP_APP_DONE = "Successfully stopped Web App.";
    public static final String START_APP_DONE = "Successfully started Web App.";
    public static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    public static final String SLOT_SHOULD_EXIST_NOW = "Target deployment slot still does not exist. " +
            "Please check if any error message during creation.";

    protected DeploymentUtil util = new DeploymentUtil();

    @Override
    protected void doExecute() throws AzureExecutionException {
        // todo: use parser to getAzureClient from mojo configs
        try {
            final RuntimeHandler runtimeHandler = getFactory().getRuntimeHandler(
                    getWebAppConfiguration(), getAzureClient());
            // todo: use parser to get web app from mojo configs
            final WebApp app = getWebApp();
            if (app == null) {
                if (this.isDeployToDeploymentSlot()) {
                    throw new AzureExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
                }
                createWebApp(runtimeHandler);
            } else {
                updateWebApp(runtimeHandler, app);
            }
            deployArtifacts(getWebAppConfiguration());
        } catch (IOException | AzureAuthFailureException | InterruptedException e) {
            throw new AzureExecutionException(
                    String.format("Encoutering error when deploying to azure: '%s'", e.getMessage()), e);
        }
    }

    protected void createWebApp(final RuntimeHandler runtimeHandler) throws AzureExecutionException {
        Log.info(WEBAPP_NOT_EXIST);

        final WithCreate withCreate = (WithCreate) runtimeHandler.defineAppWithRuntime();
        getFactory().getSettingsHandler(this).processSettings(withCreate);
        withCreate.create();

        Log.info(WEBAPP_CREATED);
    }

    protected void updateWebApp(final RuntimeHandler runtimeHandler, final WebApp app) throws AzureExecutionException, AzureAuthFailureException {
        // Update App Service Plan
        runtimeHandler.updateAppServicePlan(app);
        // Update Web App
        final Update update = (Update) runtimeHandler.updateAppRuntime(app);
        if (update == null) {
            Log.info(UPDATE_WEBAPP_SKIP);
        } else {
            Log.info(UPDATE_WEBAPP);
            getFactory().getSettingsHandler(this).processSettings(update);
            update.apply();
            Log.info(UPDATE_WEBAPP_DONE);
        }

        if (isDeployToDeploymentSlot()) {
            Log.info(CREATE_DEPLOYMENT_SLOT);

            getFactory().getDeploymentSlotHandler(this).createDeploymentSlotIfNotExist();

            Log.info(CREATE_DEPLOYMENT_SLOT_DONE);
        }
    }

    protected void deployArtifacts(WebAppConfiguration webAppConfiguration)
        throws AzureAuthFailureException, InterruptedException, AzureExecutionException, IOException {
        try {
            util.beforeDeployArtifacts();
            final WebApp app = getWebApp();
            final DeployTarget target;

            if (this.isDeployToDeploymentSlot()) {
                final String slotName = getDeploymentSlotSetting().getName();
                final DeploymentSlot slot = getDeploymentSlot(app, slotName);
                if (slot == null) {
                    throw new AzureExecutionException(SLOT_SHOULD_EXIST_NOW);
                }
                target = new DeploymentSlotDeployTarget(slot);
            } else {
                target = new WebAppDeployTarget(app);
            }
            final ArtifactHandler artifactHandler = getFactory().getArtifactHandler(this);
            final boolean isV1Schema = SchemaVersion.fromString(this.getSchemaVersion()) == SchemaVersion.V1;
            if (isV1Schema) {
                handleV1Artifact(target, this.resources, artifactHandler);
            } else {
                final List<Resource> v2Resources = this.deployment == null ? null : this.deployment.getResources();
                handleV2Artifact(target, v2Resources, artifactHandler);
            }
        } finally {
            util.afterDeployArtifacts();
        }
    }

    private void handleV2Artifact(final DeployTarget target, List<Resource> v2Resources, ArtifactHandler artifactHandler)
            throws IOException, AzureExecutionException {
        if (v2Resources == null || v2Resources.isEmpty()) {
            Log.warn("No <resources> is found in <deployment> element in pom.xml, skip deployment.");
            return;
        }
        final Map<Boolean, List<Resource>> resourceMap = v2Resources.stream()
                .collect(Collectors.partitioningBy(DeployMojo::isExternalResource));
        deployExternalResources(target, resourceMap.get(true));
        copyArtifactsToStagingDirectory(resourceMap.get(false));
        if (resourceMap.get(false).isEmpty()) {
            Log.info("All external resources are already deployed.");
            return;
        }
        artifactHandler.publish(target);
    }

    private void handleV1Artifact(final DeployTarget target, List<Resource> v1Resources, final ArtifactHandler artifactHandler)
            throws AzureExecutionException, IOException {
        if (v1Resources == null || v1Resources.isEmpty()) {
            // TODO: v1 schema will be deprecated, so this legacy code will be removed in future
            if (!(artifactHandler instanceof NONEArtifactHandlerImpl ||
                    artifactHandler instanceof JarArtifactHandlerImpl ||
                    artifactHandler instanceof WarArtifactHandlerImpl
                    )) {
                throw new AzureExecutionException(NO_RESOURCES_CONFIG);
            }
        } else {
            copyArtifactsToStagingDirectory(v1Resources);
        }
        artifactHandler.publish(target);
    }

    private HandlerFactory getFactory() {
        return HandlerFactory.getInstance();
    }

    private void copyArtifactsToStagingDirectory(List<Resource> resourceList) throws IOException, AzureExecutionException {
        if (resourceList.isEmpty()) {
            return;
        }
        Utils.prepareResources(this.getProject(), this.getSession(), this.getMavenResourcesFiltering(),
                resourceList, getDeploymentStagingDirectoryPath());
    }

    private void deployExternalResources(DeployTarget deployTarget, List<Resource> externalResources) throws AzureExecutionException {
        if (externalResources.isEmpty()) {
            return;
        }
        final PublishingProfile publishingProfile = deployTarget.getPublishingProfile();
        final String serverUrl = publishingProfile.ftpUrl().split("/", 2)[0];
        try {
            final FTPClient ftpClient = FTPUtils.getFTPClient(serverUrl, publishingProfile.ftpUsername(), publishingProfile.ftpPassword());
            for (final Resource externalResource : externalResources) {
                uploadResource(externalResource, ftpClient);
            }
        } catch (IOException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }

    private void uploadResource(Resource resource, FTPClient ftpClient) throws IOException {
        final List<File> files = Utils.getArtifacts(resource);
        final String target = getAbsoluteTargetPath(resource.getTargetPath());
        for (final File file : files) {
            FTPUtils.uploadFile(ftpClient, file.getPath(), target);
        }
    }

    private static String getAbsoluteTargetPath(String targetPath) {
        // convert null to empty string
        targetPath = StringUtils.defaultString(targetPath);
        return StringUtils.startsWith(targetPath, "/") ? targetPath :
                FTP_ROOT.resolve(Paths.get(targetPath)).normalize().toString();
    }

    private static boolean isExternalResource(Resource resource) {
        final Path target = Paths.get(getAbsoluteTargetPath(resource.getTargetPath()));
        return !target.startsWith(FTP_ROOT);
    }

    class DeploymentUtil {
        boolean isAppStopped = false;

        public void beforeDeployArtifacts() throws AzureAuthFailureException, InterruptedException {
            if (isStopAppDuringDeployment()) {
                Log.info(STOP_APP);

                getWebApp().stop();

                // workaround for the resources release problem.
                // More details: https://github.com/Microsoft/azure-maven-plugins/issues/191
                TimeUnit.SECONDS.sleep(10 /* 10 seconds */);

                isAppStopped = true;

                Log.info(STOP_APP_DONE);
            }
        }

        public void afterDeployArtifacts() throws AzureAuthFailureException, IOException {
            if (isAppStopped) {
                Log.info(START_APP);

                getWebApp().start();
                isAppStopped = false;

                Log.info(START_APP_DONE);
            }
            if (stagingDirectory != null) {
                FileUtils.forceDeleteOnExit(stagingDirectory);
            }
        }
    }
}
