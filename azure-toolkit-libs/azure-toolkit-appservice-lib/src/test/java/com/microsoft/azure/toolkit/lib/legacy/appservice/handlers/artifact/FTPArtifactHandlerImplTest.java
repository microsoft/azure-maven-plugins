/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTargetType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
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
        handlerSpy = Mockito.spy(handler);
    }

    @Test
    public void publishWebApp() throws IOException, AzureExecutionException {
        buildHandler();

        final WebApp app = Mockito.mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);

        Mockito.doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        Mockito.doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        Mockito.verify(handlerSpy, Mockito.times(1)).assureStagingDirectoryNotEmpty();
        Mockito.verify(handlerSpy, Mockito.times(1)).uploadDirectoryToFTP(target);
        Mockito.verify(handlerSpy, Mockito.times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishWebAppDeploymentSlot() throws IOException, AzureExecutionException {
        buildHandler();

        final DeploymentSlot slot = Mockito.mock(DeploymentSlot.class);
        final DeployTarget target = new DeployTarget(slot, DeployTargetType.SLOT);

        Mockito.doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        Mockito.doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        Mockito.verify(handlerSpy, Mockito.times(1)).assureStagingDirectoryNotEmpty();
        Mockito.verify(handlerSpy, Mockito.times(1)).uploadDirectoryToFTP(target);
        Mockito.verify(handlerSpy, Mockito.times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishFunctionApp() throws IOException, AzureExecutionException {
        buildHandler();

        final FunctionApp app = Mockito.mock(FunctionApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.FUNCTION);

        Mockito.doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        Mockito.doNothing().when(handlerSpy).uploadDirectoryToFTP(target);

        handlerSpy.publish(target);

        Mockito.verify(handlerSpy, Mockito.times(1)).assureStagingDirectoryNotEmpty();
        Mockito.verify(handlerSpy, Mockito.times(1)).uploadDirectoryToFTP(target);
        Mockito.verify(handlerSpy, Mockito.times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void uploadDirectoryToFTP() throws Exception {
        final String ftpUrl = "ftp.azurewebsites.net/site/wwwroot";
        final PublishingProfile profile = Mockito.mock(PublishingProfile.class);
        final WebApp app = Mockito.mock(WebApp.class);
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.WEBAPP);
        final FTPUploader uploader = Mockito.mock(FTPUploader.class);
        Mockito.doReturn(ftpUrl).when(profile).ftpUrl();
        Mockito.doReturn(profile).when(app).getPublishingProfile();

        buildHandler();
        doReturn(uploader).when(handlerSpy).getUploader();
        handlerSpy.uploadDirectoryToFTP(deployTarget);

        Mockito.verify(app, Mockito.times(1)).getPublishingProfile();
        Mockito.verifyNoMoreInteractions(app);
        Mockito.verify(profile, Mockito.times(1)).ftpUrl();
        Mockito.verify(profile, Mockito.times(1)).ftpUsername();
        Mockito.verify(profile, Mockito.times(1)).ftpPassword();
        Mockito.verifyNoMoreInteractions(profile);
        Mockito.verify(uploader, Mockito.times(1))
                .uploadDirectoryWithRetries("ftp.azurewebsites.net", null, null,
                        "target/classes", "/site/wwwroot", 3);
        verifyNoMoreInteractions(uploader);
    }
}
