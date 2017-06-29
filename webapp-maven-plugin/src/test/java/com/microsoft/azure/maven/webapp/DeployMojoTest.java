/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApps;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.handlers.DeployHandler;
import com.microsoft.azure.maven.webapp.handlers.PrivateDockerHubDeployHandler;
import com.microsoft.azure.maven.webapp.handlers.PrivateDockerRegistryDeployHandler;
import com.microsoft.azure.maven.webapp.handlers.PublicDockerHubDeployHandler;
import com.microsoft.azure.maven.telemetry.AppInsightsProxy;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;
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

    @Mock
    Azure azure;

    @Mock
    WebApps webApps;

    @Mock
    WebApp app;

    @Mock
    DeployHandler deployHandler;

    @Mock
    TelemetryProxy proxy;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(azure.webApps()).thenReturn(webApps);
    }

    @Test
    public void testGetWebAppProperties() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-public-docker-hub.xml");
        assertNotNull(mojo);

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());

        assertEquals(PricingTier.STANDARD_S1, mojo.getPricingTier());

        assertEquals("webapp-maven-plugin", mojo.getPluginName());
    }

    @Test
    public void testGetContainerSetting() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-public-docker-hub.xml");
        assertNotNull(mojo);

        final ContainerSetting containerSetting = mojo.getContainerSetting();
        assertNotNull(containerSetting);
        assertEquals("nginx", containerSetting.getImageName());
    }

    @Test
    public void testGetAppSetting() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-public-docker-hub.xml");
        assertNotNull(mojo);

        final Map map = new HashMap();
        map.put("PORT", "80");

        Assert.assertNotNull(mojo.getAppSettings());
        assertEquals(map, mojo.getAppSettings());
    }

    @Test
    public void testGetWebApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-public-docker-hub.xml");
        assertNotNull(mojo);

        ReflectionTestUtils.setField(mojo, "azure", azure);
        when(azure.webApps()).thenReturn(webApps);
        when(webApps.getByResourceGroup(any(String.class), any(String.class))).thenReturn(app);

        assertEquals(app, mojo.getWebApp());
    }

    @Test
    public void testExecuteWithDeploy() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-public-docker-hub.xml");
        assertNotNull(mojo);

        final DeployMojo mojoSpy = spy(mojo);
        final OperationResult result = new OperationResult(true, null);
        doCallRealMethod().when(mojoSpy).getWebApp();
        doCallRealMethod().when(mojoSpy).deploy();
        doReturn(deployHandler).when(mojoSpy).getDeployHandler();
        when(deployHandler.validate(app)).thenReturn(result);
        when(deployHandler.deploy(app)).thenReturn(result);
        when(webApps.getByResourceGroup(any(String.class), any(String.class))).thenReturn(app);
        ReflectionTestUtils.setField(mojoSpy, "azure", azure);

        mojoSpy.execute();

        verify(deployHandler, times(1)).validate(app);
        verify(deployHandler, times(1)).deploy(app);
    }

    @Test
    public void testExecuteWithDeploySkipped() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-no-container-setting.xml");
        assertNotNull(mojo);

        final DeployMojo mojoSpy = spy(mojo);
        doReturn(null).when(mojoSpy).getDeployHandler();
        ReflectionTestUtils.setField(mojoSpy, "azure", azure);

        mojoSpy.execute();
    }

    @Test
    public void testNoDeployHandlerFound() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-no-container-setting.xml");
        assertNotNull(mojo);

        assertNull(mojo.getDeployHandler());
    }

    @Test
    public void testDeployWithPublicDockerImage() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-public-docker-hub.xml");
        assertNotNull(mojo);

        assertThat(mojo.getDeployHandler(), instanceOf(PublicDockerHubDeployHandler.class));
    }

    @Test
    public void testDeployWebAppWithPrivateDockerImage() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-private-docker-hub.xml");
        assertNotNull(mojo);

        assertThat(mojo.getDeployHandler(), instanceOf(PrivateDockerHubDeployHandler.class));
    }

    @Test
    public void testDeployWebAppWithPrivateRegistryImage() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-private-docker-registry.xml");
        assertNotNull(mojo);

        assertThat(mojo.getDeployHandler(), instanceOf(PrivateDockerRegistryDeployHandler.class));
    }

    private DeployMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(DeployMojoTest.class.getResource(filename).toURI());
        return (DeployMojo) rule.lookupMojo("deploy", pom);
    }
}
