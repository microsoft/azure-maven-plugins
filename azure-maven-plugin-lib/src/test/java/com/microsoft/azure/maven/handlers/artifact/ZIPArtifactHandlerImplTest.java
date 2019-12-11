/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.zeroturnaround.zip.ZipException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ZIPArtifactHandlerImplTest {
    @Mock
    private AbstractAppServiceMojo mojo;

    private ZIPArtifactHandlerImpl.Builder builder = new ZIPArtifactHandlerImpl.Builder();

    private ZIPArtifactHandlerImpl handler;
    private ZIPArtifactHandlerImpl handlerSpy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void buildHandler() {
        handler = builder.stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath()).log(mojo.getLog()).build();
        handlerSpy = spy(handler);
    }

    @Test
    public void publish() throws AzureExecutionException, IOException {
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        final File file = mock(File.class);
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());
        buildHandler();

        doReturn(file).when(handlerSpy).getZipFile();
        doNothing().when(app).zipDeploy(file);
        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();
        doReturn(false).when(handlerSpy).isResourcesPreparationRequired(target);

        handlerSpy.publish(target);

        verify(handlerSpy, times(0)).prepareResources();
        verify(handlerSpy, times(1)).isResourcesPreparationRequired(target);
        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verify(handlerSpy, times(1)).getZipFile();
        verify(handlerSpy, times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishThrowException() throws AzureExecutionException, IOException {
        final Log log = mock(Log.class);
        doReturn(log).when(mojo).getLog();
        doNothing().when(log).info(anyString());
        buildHandler();
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        final File file = mock(File.class);

        doReturn(file).when(handlerSpy).getZipFile();
        doReturn(false).when(handlerSpy).isResourcesPreparationRequired(target);
        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();

        try {
            handlerSpy.publish(target);
        } catch (final AzureExecutionException e) {
            assertEquals("The zip deploy failed after 3 times of retry.", e.getMessage());
        }
    }

    @Test
    public void publishThrowResourceNotConfiguredException() throws IOException {
        buildHandler();
        final WebApp app = mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);

        try {
            handlerSpy.publish(target);
        } catch (final AzureExecutionException e) {
            assertEquals("<resources> is empty. Please make sure it is configured in pom.xml.",
                e.getMessage());
        }
    }

    @Test
    public void getZipFile() {
        final File zipTestDirectory = new File("src/test/resources/ziptest");
        doReturn(zipTestDirectory.getAbsolutePath()).when(mojo).getDeploymentStagingDirectoryPath();
        buildHandler();
        assertEquals(zipTestDirectory.getAbsolutePath() + ".zip", handlerSpy.getZipFile().getAbsolutePath());
    }

    @Test(expected = ZipException.class)
    public void getZipFileThrowException() {
        doReturn("").when(mojo).getDeploymentStagingDirectoryPath();
        buildHandler();
        handlerSpy.getZipFile();
    }
}
