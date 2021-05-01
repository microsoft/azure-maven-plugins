/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.handlers.artifact;

import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeployTarget;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class FTPArtifactHandlerImplTest {

    private FTPArtifactHandlerImpl.Builder builder = new FTPArtifactHandlerImpl.Builder();

    private FTPArtifactHandlerImpl handler = null;

    private FTPArtifactHandlerImpl handlerSpy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void buildHandler() {
        handler = builder.stagingDirectoryPath("target/classes").build();
        handlerSpy = spy(handler);
    }

    @Test
    public void publishWebApp() throws IOException, AzureExecutionException {
        buildHandler();

        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);

        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(target);
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishWebAppDeploymentSlot() throws IOException, AzureExecutionException {
        buildHandler();

        final DeploymentSlot slot = mock(DeploymentSlot.class);
        final DeployTarget target = new DeployTarget(slot, DeployTargetType.SLOT);

        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(target);
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishFunctionApp() throws IOException, AzureExecutionException {
        buildHandler();

        final FunctionApp app = mock(FunctionApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.FUNCTION);

        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(target);
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void uploadDirectoryToFTP() throws Exception {
        final String ftpUrl = "ftp.azurewebsites.net/site/wwwroot";
        final PublishingProfile profile = mock(PublishingProfile.class);
        final WebApp app = mock(WebApp.class);
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.WEBAPP);
        final FTPUploader uploader = mock(FTPUploader.class);
        doReturn(ftpUrl).when(profile).ftpUrl();
        doReturn(profile).when(app).getPublishingProfile();

        buildHandler();
        doReturn(uploader).when(handlerSpy).getUploader();
        handlerSpy.uploadDirectoryToFTP(deployTarget);

        verify(app, times(1)).getPublishingProfile();
        verifyNoMoreInteractions(app);
        verify(profile, times(1)).ftpUrl();
        verify(profile, times(1)).ftpUsername();
        verify(profile, times(1)).ftpPassword();
        verifyNoMoreInteractions(profile);
        verify(uploader, times(1))
            .uploadDirectoryWithRetries("ftp.azurewebsites.net", null, null,
                "target/classes", "/site/wwwroot", 3);
        verifyNoMoreInteractions(uploader);
    }
}
