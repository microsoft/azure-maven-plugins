/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentType;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Map;

import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.DEPLOYMENT_TYPE_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.DEPLOY_TO_SLOT_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.DOCKER_IMAGE_TYPE_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.JAVA_VERSION_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.JAVA_WEB_CONTAINER_KEY;
import static com.microsoft.azure.maven.webapp.AbstractWebAppMojo.OS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() {
        }

        @Override
        protected void after() {
        }
    };

    @Mock
    protected PluginDescriptor plugin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getConfigurationForLinux() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());

        assertNull(mojo.getPricingTier());

        assertEquals(1, mojo.getAppSettings().size());

        assertEquals(DeploymentType.EMPTY, mojo.getDeploymentType());

        assertEquals(1, mojo.getDeployment().getResources().size());

        assertFalse(mojo.isStopAppDuringDeployment());
    }

    @Test
    public void getConfigurationForWindows() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-windows.xml");

        assertEquals("Java 11", mojo.getRuntime().getJavaVersion());

        assertEquals(PricingTier.STANDARD_S2, PricingTier.fromString(mojo.getPricingTier()));
    }

    @Test
    public void getTelemetryProperties() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom-linux.xml");
        final DeployMojo spyMojo = spy(mojo);
        ReflectionUtils.setVariableValueInObject(spyMojo, "plugin", plugin);
        doReturn("azure-webapp-maven-plugin").when(plugin).getArtifactId();
        final Map map = spyMojo.getTelemetryProperties();
        assertEquals(13, map.size());
        assertTrue(map.containsKey(JAVA_VERSION_KEY));
        assertTrue(map.containsKey(JAVA_WEB_CONTAINER_KEY));
        assertTrue(map.containsKey(DOCKER_IMAGE_TYPE_KEY));
        assertTrue(map.containsKey(DEPLOYMENT_TYPE_KEY));
        assertTrue(map.containsKey(OS_KEY));
        assertTrue(map.containsKey(DEPLOY_TO_SLOT_KEY));
    }

    /**
     * refer https://stackoverflow.com/questions/44009232/nosuchelementexception-thrown-while-testing-maven-plugin
     */
    private DeployMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(DeployMojoTest.class.getResource(filename).toURI());
        final PlexusConfiguration config = rule.extractPluginConfiguration("azure-webapp-maven-plugin", pom);
        final DeployMojo mojo = (DeployMojo) rule.configureMojo(new DeployMojo(), config);
        assertNotNull(mojo);
        return mojo;
    }
}
