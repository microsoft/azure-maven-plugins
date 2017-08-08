/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.function.DeployMojo;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FTPArtifactHandlerImplTest {
    @Test
    public void publish() throws Exception {
        final String ftpUrl = "ftp.azurewebsites.net/site/wwwroot";
        final PublishingProfile profile = mock(PublishingProfile.class);
        when(profile.ftpUrl()).thenReturn(ftpUrl);
        final FunctionApp app = mock(FunctionApp.class);
        when(app.getPublishingProfile()).thenReturn(profile);
        final DeployMojo mojo = mock(DeployMojo.class);
        when(mojo.getFunctionApp()).thenReturn(app);
        final FTPUploader uploader = mock(FTPUploader.class);
        final FTPArtifactHandlerImpl handler = new FTPArtifactHandlerImpl(mojo);
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        doReturn(uploader).when(handlerSpy).getUploader();

        handlerSpy.publish();

        verify(handlerSpy, times(1)).getUploader();
        verify(handlerSpy, times(1)).publish();
        verifyNoMoreInteractions(handlerSpy);
        verify(mojo, times(1)).getFunctionApp();
        verify(mojo, times(1)).getDeploymentStageDirectory();
        verifyNoMoreInteractions(mojo);
        verify(app, times(1)).getPublishingProfile();
        verifyNoMoreInteractions(app);
        verify(profile, times(1)).ftpUrl();
        verify(profile, times(1)).ftpUsername();
        verify(profile, times(1)).ftpPassword();
        verifyNoMoreInteractions(profile);
        verify(uploader, times(1))
                .uploadDirectoryWithRetries(anyString(), isNull(), isNull(), isNull(), anyString(), anyInt());
        verifyNoMoreInteractions(uploader);
    }

    @Test
    public void getUploader() throws Exception {
        final DeployMojo mojo = mock(DeployMojo.class);
        final FTPArtifactHandlerImpl handler = new FTPArtifactHandlerImpl(mojo);
        assertTrue(handler.getUploader() instanceof FTPUploader);
    }
}
