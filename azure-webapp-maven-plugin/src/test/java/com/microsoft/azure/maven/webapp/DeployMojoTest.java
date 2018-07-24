/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.artifacthandler.IArtifactHandler;
import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import com.microsoft.azure.maven.webapp.handlers.DeploymentSlotHandler;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.SettingsHandler;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
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
    protected IArtifactHandler artifactHandler;

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
            public RuntimeHandler getRuntimeHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return runtimeHandler;
            }

            @Override
            public SettingsHandler getSettingsHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
                return settingsHandler;
            }

            @Override
            public IArtifactHandler getArtifactHandler(AbstractWebAppMojo mojo) throws MojoExecutionException {
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

        assertEquals(PricingTier.STANDARD_S1, mojo.getPricingTier());

        assertEquals(null, mojo.getJavaVersion());

        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, mojo.getJavaWebContainer());

        assertFalse(mojo.getContainerSettings().isEmpty());

        assertEquals(1, mojo.getAppSettings().size());

        assertEquals(DeploymentType.NONE, mojo.getDeploymentType());

        assertEquals(1, mojo.getResources().size());

        assertFalse(mojo.isStopAppDuringDeployment());
    }

    @Test
    public void getConfigurationForWindows() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-windows.xml");

        assertEquals(JavaVersion.JAVA_8_NEWEST, mojo.getJavaVersion());

        assertEquals(WebContainer.TOMCAT_8_5_NEWEST, mojo.getJavaWebContainer());

        assertEquals(PricingTier.STANDARD_S2, mojo.getPricingTier());
    }

    @Test
    public void getTelemetryProperties() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        ReflectionUtils.setVariableValueInObject(mojo, "plugin", plugin);

        final Map map = mojo.getTelemetryProperties();

        assertEquals(11, map.size());
        assertTrue(map.containsKey(JAVA_VERSION_KEY));
        assertTrue(map.containsKey(JAVA_WEB_CONTAINER_KEY));
        assertTrue(map.containsKey(DOCKER_IMAGE_TYPE_KEY));
        assertTrue(map.containsKey(DEPLOYMENT_TYPE_KEY));
        assertTrue(map.containsKey(LINUX_RUNTIME_KEY));
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
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);

        doReturn(app).when(mojoSpy).getWebApp();

        assertNull(mojoSpy.getDeploymentSlot(app, ""));
    }

    @Test
    public void getDeploymentSlot() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-slot.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);

        doReturn(app).when(mojoSpy).getWebApp();
        doReturn(mock(DeploymentSlot.class)).when(mojoSpy).getDeploymentSlot(app, "");

        mojoSpy.getDeploymentSlot(app, "");

        verify(mojoSpy, times(1)).getDeploymentSlot(app, "");
    }

    @Test
    public void createOrUpdateWebAppWithCreate() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(null).when(mojoSpy).getWebApp();
        doNothing().when(mojoSpy).createWebApp();

        mojoSpy.createOrUpdateWebApp();

        verify(mojoSpy, times(1)).createWebApp();
        verify(mojoSpy, times(0)).updateWebApp(any(WebApp.class));
    }

    @Test
    public void createOrUpdateWebAppWithUpdate() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);
        doReturn(app).when(mojoSpy).getWebApp();
        doNothing().when(mojoSpy).updateWebApp(any(WebApp.class));

        mojoSpy.createOrUpdateWebApp();

        verify(mojoSpy, times(0)).createWebApp();
        verify(mojoSpy, times(1)).updateWebApp(any(WebApp.class));
    }

    @Test
    public void doExecute() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        doNothing().when(mojoSpy).createOrUpdateWebApp();
        doNothing().when(mojoSpy).deployArtifacts();

        mojoSpy.doExecute();

        verify(mojoSpy, times(1)).createOrUpdateWebApp();
        verify(mojoSpy, times(1)).deployArtifacts();
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
    public void createWebApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(runtimeHandler).defineAppWithRuntime();

        mojoSpy.createWebApp();

        verify(runtimeHandler, times(1)).defineAppWithRuntime();
        verify(settingsHandler, times(1)).processSettings(any(WithCreate.class));
        verify(withCreate, times(1)).create();
    }

    @Test
    public void updateWebApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);
        final Update update = mock(Update.class);
        doReturn(update).when(runtimeHandler).updateAppRuntime(app);

        mojoSpy.updateWebApp(app);

        verify(runtimeHandler, times(1)).updateAppRuntime(app);
        verify(settingsHandler, times(1)).processSettings(any(Update.class));
        verify(update, times(1)).apply();
    }

    @Test
    public void deployArtifactsWithResources() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo mojoSpy = spy(mojo);
        final WebApp app = mock(WebApp.class);

        doReturn(app).when(mojoSpy).getWebApp();
        doReturn(false).when(mojoSpy).isDeployToDeploymentSlot();

        final BaseDeployTarget deployTarget = new WebAppDeployTarget(app);
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

        final BaseDeployTarget deployTarget = new DeploymentSlotDeployTarget(slot);

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
