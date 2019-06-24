/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import com.microsoft.azure.maven.webapp.handlers.DeploymentSlotHandler;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.SettingsHandler;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.DEPLOYMENT_TYPE_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.DOCKER_IMAGE_TYPE_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.JAVA_VERSION_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.JAVA_WEB_CONTAINER_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.LINUX_RUNTIME_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.OS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
    protected PluginDescriptor plugin;

    @Mock
    protected ArtifactHandler artifactHandler;

    @Mock
    protected RuntimeHandler runtimeHandler;

    @Mock
    protected SettingsHandler settingsHandler;

    @Mock
    protected DeploymentSlotHandler deploymentSlotHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        setupHandlerFactory();
    }

    protected void setupHandlerFactory() throws Exception {
        final Field f = HandlerFactory.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, new HandlerFactory() {
            @Override
            public RuntimeHandler getRuntimeHandler(WebAppConfiguration config, Azure azureClient, Log log) {
                return runtimeHandler;
            }

            @Override
            public SettingsHandler getSettingsHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return settingsHandler;
            }

            @Override
            public ArtifactHandler getArtifactHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return artifactHandler;
            }

            @Override
            public DeploymentSlotHandler getDeploymentSlotHandler(AbstractWebAppMojo mojo)
                    throws MojoExecutionException {
                return deploymentSlotHandler;
            }
        });
    }

    @Test
    public void getConfigurationForLinux() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());

        assertEquals(null, mojo.getPricingTier());

        assertEquals(null, mojo.getJavaVersion());

        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, mojo.getJavaWebContainer());

        assertFalse(mojo.getContainerSettings().isEmpty());

        assertEquals(1, mojo.getAppSettings().size());

        assertEquals(DeploymentType.EMPTY, mojo.getDeploymentType());

        assertEquals(1, mojo.getResources().size());

        assertFalse(mojo.isStopAppDuringDeployment());
    }

    @Test(expected = MojoExecutionException.class)
    public void getDeploymentTypeThrowException() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-slot.xml");
        mojo.getDeploymentType();
    }

    @Test
    public void getConfigurationForWindows() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-windows.xml");

        assertEquals(JavaVersion.JAVA_8_NEWEST, JavaVersion.fromString(mojo.getJavaVersion()));

        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, mojo.getJavaWebContainer());

        assertEquals(PricingTier.STANDARD_S2, AppServiceUtils.getPricingTierFromString(mojo.getPricingTier()));
    }

    @Test
    public void getTelemetryProperties() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        ReflectionUtils.setVariableValueInObject(mojo, "plugin", plugin);
        doReturn("azure-webapp-maven-plugin").when(plugin).getArtifactId();

        final Map map = mojo.getTelemetryProperties();

        assertEquals(13, map.size());
        assertTrue(map.containsKey(JAVA_VERSION_KEY));
        assertTrue(map.containsKey(JAVA_WEB_CONTAINER_KEY));
        assertTrue(map.containsKey(DOCKER_IMAGE_TYPE_KEY));
        assertTrue(map.containsKey(DEPLOYMENT_TYPE_KEY));
        assertTrue(map.containsKey(LINUX_RUNTIME_KEY));
        assertTrue(map.containsKey(OS_KEY));
    }

    @Test
    public void getWebApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");

        assertNull(mojo.getWebApp());
    }

    @Test
    public void getDeploymentSlotSetting() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-slot.xml");
        assertNotNull(mojo.getDeploymentSlotSetting());
    }

    @Test
    public void getNUllDeploymentSlotSetting() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        assertNull(mojo.getDeploymentSlotSetting());
    }

    @Test
    public void getNUllDeploymentSlot() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final WebApp app = mock(WebApp.class);

        assertNull(mojo.getDeploymentSlot(app, ""));
    }

    @Test
    public void getDeploymentSlot() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-slot.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);

        doReturn(mock(DeploymentSlot.class)).when(mojoSpy).getDeploymentSlot(app, "");

        mojoSpy.getDeploymentSlot(app, "");

        verify(mojoSpy, times(1)).getDeploymentSlot(app, "");
    }

    @Test
    public void deployArtifactsWithNoResources() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);

        doReturn(app).when(mojoSpy).getWebApp();
        doReturn(false).when(mojoSpy).isDeployToDeploymentSlot();

        mojoSpy.deployArtifacts();
    }

    @Test
    public void deployArtifactsWithResources() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);

        doReturn(app).when(mojoSpy).getWebApp();
        doReturn(false).when(mojoSpy).isDeployToDeploymentSlot();

        final DeployTarget deployTarget = new WebAppDeployTarget(app);
        mojoSpy.deployArtifacts();

        verify(artifactHandler, times(1)).publish(refEq(deployTarget));
        verifyNoMoreInteractions(artifactHandler);
    }

    @Test
    public void deployToDeploymentSlot() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-slot.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final DeploymentSlot slot = mock(DeploymentSlot.class);
        final WebApp app = mock(WebApp.class);
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);
        doReturn(app).when(mojoSpy).getWebApp();
        doReturn(slotSetting).when(mojoSpy).getDeploymentSlotSetting();
        doReturn("test").when(slotSetting).getName();
        doReturn(slot).when(mojoSpy).getDeploymentSlot(app, "test");

        final DeployTarget deployTarget = new DeploymentSlotDeployTarget(slot);

        mojoSpy.deployArtifacts();

        verify(artifactHandler, times(1)).publish(refEq(deployTarget));
        verifyNoMoreInteractions(artifactHandler);
    }

    private DeployMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(DeployMojoTest.class.getResource(filename).toURI());
        final DeployMojo mojo = (DeployMojo) rule.lookupMojo("deploy", pom);
        assertNotNull(mojo);
        return mojo;
    }

    private List<Resource> getResourceList() {
        final Resource resource = new Resource();
        resource.setDirectory("/");
        resource.setTargetPath("/");

        final List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        return resources;
    }
}
