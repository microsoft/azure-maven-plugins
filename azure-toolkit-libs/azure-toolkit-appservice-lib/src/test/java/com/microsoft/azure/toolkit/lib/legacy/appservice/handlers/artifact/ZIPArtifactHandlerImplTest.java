/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact;

import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTargetType;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.management.appservice.WebApp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.zeroturnaround.zip.ZipException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ZIPArtifactHandlerImplTest {
    private ZIPArtifactHandlerImpl.Builder builder = new ZIPArtifactHandlerImpl.Builder();

    private ZIPArtifactHandlerImpl handler;
    private ZIPArtifactHandlerImpl handlerSpy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void buildHandler() {
        handler = builder.stagingDirectoryPath("src/test/resources/ziptest").build();
        handlerSpy = Mockito.spy(handler);
    }

    @Test
    public void publish() throws AzureExecutionException, IOException {
        final WebApp app = Mockito.mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        final File file = Mockito.mock(File.class);
        buildHandler();

        Mockito.doReturn(file).when(handlerSpy).getZipFile();
        Mockito.doNothing().when(app).zipDeploy(file);
        Mockito.doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();

        handlerSpy.publish(target);
        Mockito.verify(handlerSpy, Mockito.times(1)).assureStagingDirectoryNotEmpty();
        Mockito.verify(handlerSpy, Mockito.times(1)).getZipFile();
        Mockito.verify(handlerSpy, Mockito.times(1)).publish(target);
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void publishThrowException() throws AzureExecutionException, IOException {
        buildHandler();
        final WebApp app = Mockito.mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);
        final File file = Mockito.mock(File.class);

        Mockito.doReturn(file).when(handlerSpy).getZipFile();
        Mockito.doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();

        try {
            handlerSpy.publish(target);
        } catch (final AzureExecutionException e) {
            Assert.assertEquals("The zip deploy failed after 3 times of retry.", e.getMessage());
        }
    }

    @Test
    public void publishThrowResourceNotConfiguredException() throws IOException {
        buildHandler();
        final WebApp app = Mockito.mock(WebApp.class);
        final DeployTarget target = new DeployTarget(app, DeployTargetType.WEBAPP);

        try {
            handlerSpy.publish(target);
        } catch (final AzureExecutionException e) {
            Assert.assertEquals("<resources> is empty. Please make sure it is configured in pom.xml.",
                e.getMessage());
        }
    }

    @Test
    public void getZipFile() {
        final File zipTestDirectory = new File("src/test/resources/ziptest");
        buildHandler();
        assertEquals(zipTestDirectory.getAbsolutePath() + ".zip", handlerSpy.getZipFile().getAbsolutePath());
    }

    @Test(expected = ZipException.class)
    public void getZipFileThrowException() {
        handler = builder.stagingDirectoryPath("").build();
        handlerSpy = Mockito.spy(handler);
        handlerSpy.getZipFile();
    }
}
