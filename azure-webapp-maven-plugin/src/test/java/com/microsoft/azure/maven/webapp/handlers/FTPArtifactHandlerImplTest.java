/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.deployadapter.DeploymentSlotAdapter;
import com.microsoft.azure.maven.webapp.deployadapter.IDeployAdapter;
import com.microsoft.azure.maven.webapp.deployadapter.WebAppAdapter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FTPArtifactHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private FTPArtifactHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new FTPArtifactHandlerImpl(mojo);
    }

    @Test
    public void publish() throws Exception {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final IDeployAdapter deployTarget = new WebAppAdapter(this.mojo.getWebApp());
        doNothing().when(handlerSpy).copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        doNothing().when(handlerSpy).uploadDirectoryToFTP(deployTarget);

        final List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource());
        doReturn(resourceList).when(mojo).getResources();
        handlerSpy.publish(deployTarget);
        verify(handlerSpy, times(1))
                .copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(deployTarget);
    }

    @Test
    public void publishToDeploymentSlot() throws Exception {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);
        doReturn(app).when(mojo).getWebApp();
        doReturn(slotSetting).when(mojo).getDeploymentSlotSetting();
        doReturn("").when(slotSetting).getSlotName();
        final IDeployAdapter deployTarget = new DeploymentSlotAdapter(this.mojo.getDeploymentSlot(app, ""));
        doNothing().when(handlerSpy).copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        doNothing().when(handlerSpy).uploadDirectoryToFTP(deployTarget);

        final List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource());
        doReturn(resourceList).when(mojo).getResources();
        handlerSpy.publish(deployTarget);
        verify(handlerSpy, times(1))
                .copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        verify(handlerSpy, times(1)).uploadDirectoryToFTP(deployTarget);
    }

    @Test
    public void copyResourcesToStageDirectory() throws Exception {
        doReturn(mock(MavenProject.class)).when(mojo).getProject();
        doReturn(mock(MavenSession.class)).when(mojo).getSession();
        doReturn(mock(MavenResourcesFiltering.class)).when(mojo).getMavenResourcesFiltering();
        doReturn("stageDirectory").when(mojo).getDeploymentStageDirectory();

        handler.copyResourcesToStageDirectory(new ArrayList<Resource>());
        verify(mojo, times(1)).getProject();
        verify(mojo, times(1)).getSession();
        verify(mojo, times(1)).getMavenResourcesFiltering();
        verify(mojo, times(1)).getDeploymentStageDirectory();
        verifyNoMoreInteractions(mojo);
    }

    @Test
    public void uploadDirectoryToFTP() throws Exception {
        final String ftpUrl = "ftp.azurewebsites.net/site/wwwroot";
        final PublishingProfile profile = mock(PublishingProfile.class);
        doReturn(ftpUrl).when(profile).ftpUrl();
        final WebApp app = mock(WebApp.class);
        doReturn(profile).when(app).getPublishingProfile();
        final DeployMojo mojo = mock(DeployMojo.class);
        doReturn(app).when(mojo).getWebApp();
        final FTPUploader uploader = mock(FTPUploader.class);
        final FTPArtifactHandlerImpl handler = new FTPArtifactHandlerImpl(mojo);
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        final IDeployAdapter deployTarget = new WebAppAdapter(mojo.getWebApp());
        doReturn(uploader).when(handlerSpy).getUploader();

        handlerSpy.uploadDirectoryToFTP(deployTarget);
        verify(mojo, times(1)).getWebApp();
        verify(mojo, times(1)).getDeploymentStageDirectory();
        verifyNoMoreInteractions(mojo);
        verify(app, times(1)).getPublishingProfile();
        verifyNoMoreInteractions(app);
        verify(profile, times(1)).ftpUrl();
        verify(profile, times(1)).ftpUsername();
        verify(profile, times(1)).ftpPassword();
        verifyNoMoreInteractions(profile);
        verify(uploader, times(1))
                .uploadDirectoryWithRetries(anyString(), (String) isNull(), (String) isNull(), (String) isNull(),
                        anyString(), anyInt());
        verifyNoMoreInteractions(uploader);
    }

    @Test
    public void getUploader() throws Exception {
        assertTrue(handler.getUploader() instanceof FTPUploader);
    }
}
