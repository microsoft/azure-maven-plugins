/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    @Test
    public void getConfiguration() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom.xml");
        assertNotNull(mojo);

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());
    }

    @Test
    public void getDeploymetnStageDirectory() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom.xml");
        final DeployMojo mojoSpy = spy(mojo);
        doReturn("target").when(mojoSpy).getBuildDirectoryAbsolutePath();
        assertEquals(Paths.get("target", "azure-functions", "appName").toString(),
                mojoSpy.getDeploymentStageDirectory());
    }

    @Test
    public void getFunctionApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom.xml");
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(null).when(mojoSpy).getAzureClient();
        assertNull(mojoSpy.getFunctionApp());
    }

    @Test
    public void doExecute() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom.xml");
        assertNotNull(mojo);

        final DeployMojo mojoSpy = spy(mojo);
        doCallRealMethod().when(mojoSpy).getLog();
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        doReturn(handler).when(mojoSpy).getArtifactHandler();
        doCallRealMethod().when(mojoSpy).createFunctionAppIfNotExist();
        doCallRealMethod().when(mojoSpy).getAppName();
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();

        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).createFunctionAppIfNotExist();
        verify(mojoSpy, times(1)).doExecute();
        verify(handler, times(1)).publish();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void getArtifactHandler() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom.xml");
        assertNotNull(mojo);

        final ArtifactHandler handler = mojo.getArtifactHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof FTPArtifactHandlerImpl);
    }

    private DeployMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(DeployMojoTest.class.getResource(filename).toURI());
        return (DeployMojo) rule.lookupMojo("deploy", pom);
    }
}
