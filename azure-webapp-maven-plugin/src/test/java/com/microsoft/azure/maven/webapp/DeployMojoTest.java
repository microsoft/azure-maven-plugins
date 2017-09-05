/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetConfigurationForLinux() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        assertNotNull(mojo);

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());

        assertEquals(PricingTier.STANDARD_S1, mojo.getPricingTier());

        assertEquals(null, mojo.getJavaVersion());

        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, mojo.getJavaWebContainer());

        assertFalse(mojo.getContainerSettings().isEmpty());

        assertEquals(1, mojo.getAppSettings().size());

        assertEquals(DeploymentType.FTP, mojo.getDeploymentType());

        assertEquals(1, mojo.getResources().size());
    }

    @Test
    public void testGetConfigurationForWindows() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-windows.xml");
        assertNotNull(mojo);

        assertEquals(JavaVersion.JAVA_8_NEWEST, mojo.getJavaVersion());

        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, mojo.getJavaWebContainer());

        assertEquals(PricingTier.STANDARD_S2, mojo.getPricingTier());
    }

    @Test
    public void testGetWebApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        assertNotNull(mojo);

        assertNull(mojo.getWebApp());
    }

    @Test
    public void testGetDeployFacade() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        assertNotNull(mojo);

        // Create a new Web App
        assertTrue(mojo.getDeployFacade() instanceof DeployFacadeImplWithCreate);

        // Deploy to existing Web App
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);
        doReturn(app).when(mojoSpy).getWebApp();

        assertTrue(mojoSpy.getDeployFacade() instanceof DeployFacadeImplWithUpdate);
    }

    @Test
    public void testDoExecute() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        assertNotNull(mojo);

        final DeployMojo mojoSpy = spy(mojo);
        final DeployFacade facade = getDeployFacade();
        doReturn(facade).when(mojoSpy).getDeployFacade();
        doCallRealMethod().when(mojoSpy).getLog();

        mojoSpy.doExecute();
    }

    private DeployMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(DeployMojoTest.class.getResource(filename).toURI());
        return (DeployMojo) rule.lookupMojo("deploy", pom);
    }

    private DeployFacade getDeployFacade() {
        return new DeployFacade() {
            @Override
            public DeployFacade setupRuntime() throws Exception {
                return this;
            }

            @Override
            public DeployFacade applySettings() throws Exception {
                return this;
            }

            @Override
            public DeployFacade commitChanges() throws Exception {
                return this;
            }

            @Override
            public DeployFacade deployArtifacts() throws Exception {
                return this;
            }
        };
    }
}
