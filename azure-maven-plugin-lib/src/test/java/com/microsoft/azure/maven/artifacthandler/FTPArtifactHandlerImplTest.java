/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class FTPArtifactHandlerImplTest {
    @Mock
    private AbstractAppServiceMojo mojo;

    private FTPArtifactHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new FTPArtifactHandlerImpl(mojo);
    }

    @Test
    public void publishWebApp() throws IOException, MojoExecutionException {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);

        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doNothing().when(handlerSpy).prepareResources();
        doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).isResourcesPreparationRequired(target);
        verify(handlerSpy, times(1)).prepareResources();
        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(target);
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishWebAppDeploymentSlot() throws IOException, MojoExecutionException {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final DeploymentSlot slot = mock(DeploymentSlot.class);
        final DeployTarget target = new DeployTarget(slot, DeployTargetType.SLOT);

        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doNothing().when(handlerSpy).prepareResources();
        doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).isResourcesPreparationRequired(target);
        verify(handlerSpy, times(1)).prepareResources();
        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(target);
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishFunctionApp() throws IOException, MojoExecutionException {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final FunctionApp app = mock(FunctionApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.FUNCTION);

        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).isResourcesPreparationRequired(target);
        verify(handlerSpy, times(0)).prepareResources();
        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(target);
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void isResourcesPreparationRequired() {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);

        DeployTarget target = new DeployTarget(mock(WebApp.class), DeployTargetType.WEBAPP);
        assertTrue(handlerSpy.isResourcesPreparationRequired(target));

        target = new DeployTarget(mock(DeploymentSlot.class), DeployTargetType.SLOT);
        assertTrue(handlerSpy.isResourcesPreparationRequired(target));

        target = new DeployTarget(mock(FunctionApp.class), DeployTargetType.FUNCTION);
        assertFalse(handlerSpy.isResourcesPreparationRequired(target));
    }

    @Test
    public void uploadDirectoryToFTP() throws Exception {
        final String ftpUrl = "ftp.azurewebsites.net/site/wwwroot";
        final PublishingProfile profile = mock(PublishingProfile.class);
        final WebApp app = mock(WebApp.class);
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.WEBAPP);
        final FTPUploader uploader = mock(FTPUploader.class);
        doReturn(uploader).when(handlerSpy).getUploader();
        doReturn(ftpUrl).when(profile).ftpUrl();
        doReturn(profile).when(app).getPublishingProfile();
        doReturn("").when(mojo).getDeploymentStagingDirectoryPath();

        handlerSpy.uploadDirectoryToFTP(deployTarget);

        verify(app, times(1)).getPublishingProfile();
        verifyNoMoreInteractions(app);
        verify(profile, times(1)).ftpUrl();
        verify(profile, times(1)).ftpUsername();
        verify(profile, times(1)).ftpPassword();
        verifyNoMoreInteractions(profile);
        verify(uploader, times(1))
            .uploadDirectoryWithRetries("ftp.azurewebsites.net", null, null,
                "", "/site/wwwroot", 3);
        verifyNoMoreInteractions(uploader);
    }
}
