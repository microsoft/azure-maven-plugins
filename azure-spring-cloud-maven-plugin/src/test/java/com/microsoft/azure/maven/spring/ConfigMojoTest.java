/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.ProxyResource;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.maven.common.telemetry.AppInsightHelper;
import com.microsoft.azure.maven.spring.configuration.AppSettings;
import com.microsoft.azure.maven.spring.configuration.DeploymentSettings;
import com.microsoft.azure.maven.spring.pom.PomXmlUpdater;
import com.microsoft.azure.maven.spring.prompt.PromptWrapper;
import com.microsoft.azure.maven.spring.spring.SpringServiceClient;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({ AzureAuthHelper.class, AppInsightHelper.class, TelemetryClient.class, ConfigMojo.class, AbstractSpringMojo.class, Azure.class,
        ServiceResourceInner.class, ProxyResource.class, SpringServiceClient.class, PomXmlUpdater.class})
public class ConfigMojoTest {
    @Rule
    private MojoRule rule = new MojoRule();

    private ConfigMojo mojo;

    private Settings settings;

    private MavenProject project;

    private Model model;

    private Log mockLog;

    private ExpressionEvaluator mockEval;

    private BufferedReader reader;

    private MavenSession session;

    private PomXmlUpdater mockUpdater;

    @Before
    public void setUp() throws Exception {
        TestHelper.mockAppInsightHelper();
        final File pom = new File(this.getClass().getResource("/test.xml").getFile());
        assertNotNull(pom);
        assertTrue(pom.exists());
        mojo = (ConfigMojo) rule.lookupMojo("config", pom);
        final MojoExecution execution = rule.newMojoExecution("config");
        assertNotNull(mojo);
        mojo.plugin = execution.getMojoDescriptor().getPluginDescriptor();

        model = TestHelper.readMavenModel(pom);
        project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getModel()).thenReturn(model);
        Mockito.when(project.getPackaging()).thenReturn(model.getPackaging());
        mojo.project = project;

        settings = mock(Settings.class);
        when(settings.isInteractiveMode()).thenReturn(true);
        mojo.settings = settings;

        mockLog = mock(Log.class);
        mockEval = mock(ExpressionEvaluator.class);
        FieldUtils.writeField(mojo, "log", mockLog, true);

        mockStatic(AzureAuthHelper.class);
        final AzureTokenWrapper mockTokenCred = mock(AzureTokenWrapper.class);
        when(AzureAuthHelper.getAzureTokenCredentials(null)).thenReturn(mockTokenCred);

        reader = mock(BufferedReader.class);
        session = mock(MavenSession.class);
        mojo.session = session;

        mockUpdater = mock(PomXmlUpdater.class);
        PowerMockito.doNothing().when(mockUpdater).updateSettings(any(), any());
        whenNew(PomXmlUpdater.class).withAnyArguments().thenReturn(mockUpdater);
    }

    @Test
    public void testNonInteractiveMode() throws Exception {
        when(settings.isInteractiveMode()).thenReturn(false);
        mojo.isTelemetryAllowed = false;
        try {
            mojo.execute();
            fail("Config should not work in non-interactive mode.");
        } catch (MojoFailureException ex) {
            // expected
            assertTrue(ex.getCause() instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testQuitNonJarProject() throws Exception {
        when(project.getPackaging()).thenReturn("war");
        mojo.isTelemetryAllowed = false;
        try {
            mojo.execute();
            fail("Config should not work for packaging war.");
        } catch (MojoFailureException ex) {
            // expected
            assertTrue(ex.getCause() instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testQuitConfiguredProject() throws Exception {
        final File pom = new File(this.getClass().getResource("/configured.xml").getFile());
        model = TestHelper.readMavenModel(pom);
        Mockito.when(project.getModel()).thenReturn(model);
        mojo.isTelemetryAllowed = false;

        PowerMockito.doThrow(new RuntimeException("unit test")).when(mockLog).warn(contains("is already configured"));
        try {
            mojo.execute();
            fail("Config should not work for pom.xml already configured.");
        } catch (Exception ex) {
            assertEquals("unit test", ex.getCause().getMessage());
        }
    }

    @Test
    public void testAdvancedOptionsInParent() throws Exception {
        when(project.getPackaging()).thenReturn("pom");
        FieldUtils.writeField(mojo, "advancedOptions", true, true);
        mojo.isTelemetryAllowed = false;
        try {
            mojo.execute();
            fail("Config should not work for advancedOptions in parent pom.");
        } catch (MojoFailureException ex) {
            // expected
            assertTrue(ex.getCause().getMessage().contains("advancedOptions"));
        }
    }

    @Test
    public void testDontSelectProject() throws Exception {
        initializeParentChildModel();

        when(reader.readLine()).thenReturn("100-100000");
        mojo.execute();

        Mockito.verify(mockLog);
        mockLog.warn(contains("You have not selected any projects"));
    }

    @Test
    public void testNoSelectableProject() throws Exception {
        initializeParentChildModel();

        final List<MavenProject> parentList = Collections.singletonList(TestHelper.createParentProject());
        when(session.getAllProjects()).thenReturn(parentList);
        mojo.execute();
        Mockito.verify(mockLog);
        mockLog.warn(contains("There are no projects in current folder with package"));
    }

    @Test
    public void testSelectNoSubscription() throws Exception {
        initializeParentChildModel();

        final SpringServiceClient mockServiceClient = mock(SpringServiceClient.class);
        final List<ServiceResourceInner> serviceList = TestHelper.createServiceList();
        when(mockServiceClient.getAvailableClusters()).thenReturn(serviceList);
        whenNew(SpringServiceClient.class).withAnyArguments().thenReturn(mockServiceClient);
        TestHelper.mockAzureWithSubs(TestHelper.createMockSubscriptions(0));

        try {
            mojo.execute();
        } catch (MojoFailureException ex) {
            assertTrue(ex.getCause().getMessage().contains("Cannot find any subscriptions"));
        }
    }

    @Test
    public void testAcceptDefault() throws Exception {
        initializeParentChildModel();

        final SpringServiceClient mockServiceClient = mock(SpringServiceClient.class);
        final List<ServiceResourceInner> serviceList = TestHelper.createServiceList();
        when(mockServiceClient.getAvailableClusters()).thenReturn(serviceList);
        whenNew(SpringServiceClient.class).withAnyArguments().thenReturn(mockServiceClient);
        TestHelper.mockAzureWithSubs(TestHelper.createMockSubscriptions(2));

        when(reader.readLine()).thenReturn("1-2").thenReturn("2").thenReturn("");

        mojo.execute();

        Mockito.verify(mockUpdater, times(3));
        mockUpdater.updateSettings(any(), any());
        final AppSettings app = (AppSettings) FieldUtils.readField(mojo, "appSettings", true);
        assertEquals("new_subs_id2", app.getSubscriptionId());
        assertEquals("testCluster1", app.getClusterName());
        assertEquals("service", app.getAppName());
    }

    @Test
    public void testSingleProjectInParent() throws Exception {
        initializeParentChildModel();

        final SpringServiceClient mockServiceClient = mock(SpringServiceClient.class);
        final List<ServiceResourceInner> serviceList = TestHelper.createServiceList();
        when(mockServiceClient.getAvailableClusters()).thenReturn(serviceList);
        whenNew(SpringServiceClient.class).withAnyArguments().thenReturn(mockServiceClient);
        TestHelper.mockAzureWithSubs(TestHelper.createMockSubscriptions(2));
        // 1. select project(service);
        // 2. select subscription
        // 3. select cluster
        // y. select public
        when(reader.readLine()).thenReturn("1").thenReturn("2").thenReturn("3").thenReturn("y");

        mojo.execute();

        Mockito.verify(mockUpdater, times(2));
        mockUpdater.updateSettings(any(), any());
        final AppSettings app = (AppSettings) FieldUtils.readField(mojo, "appSettings", true);
        assertEquals("new_subs_id2", app.getSubscriptionId());
        assertEquals("testCluster3", app.getClusterName());
        assertEquals("core", app.getAppName());
        assertEquals("true", app.isPublic());
    }

    @Test
    public void testChildProject() throws Exception {
        final PluginParameterExpressionEvaluator pluginParameterExpressionEvaluator = mock(PluginParameterExpressionEvaluator.class);
        when(mockEval.evaluate("${evalBad}")).thenReturn("bad-text!!!!");
        when(mockEval.evaluate("${eval}")).thenReturn("appname");
        whenNew(PluginParameterExpressionEvaluator.class).withAnyArguments().thenReturn(pluginParameterExpressionEvaluator);
        mojo.project = TestHelper.createChildProject("service");

        final SpringServiceClient mockServiceClient = mock(SpringServiceClient.class);
        final List<ServiceResourceInner> serviceList = TestHelper.createServiceList();
        when(mockServiceClient.getAvailableClusters()).thenReturn(serviceList);
        whenNew(SpringServiceClient.class).withAnyArguments().thenReturn(mockServiceClient);
        TestHelper.mockAzureWithSubs(TestHelper.createMockSubscriptions(2));
        // 1. for subs
        // 2. cluster
        // 3. appName
        // 4. isPublic

        when(reader.readLine()).thenReturn("1").thenReturn("2").
            thenReturn("$$%#").thenReturn("${evalBad}").thenReturn("${eval}").thenReturn("y").thenReturn("");
        mojo.isTelemetryAllowed = false;
        initMockPromptWrapper();
        mojo.execute();
        final AppSettings app = (AppSettings) FieldUtils.readField(mojo, "appSettings", true);
        final DeploymentSettings deploy = (DeploymentSettings) FieldUtils.readField(mojo, "deploymentSettings", true);
        assertEquals("new_subs_id", app.getSubscriptionId());
        assertEquals("testCluster2", app.getClusterName());
        assertEquals("${eval}", app.getAppName());
        assertEquals("true", app.isPublic());
        assertEquals("1", deploy.getCpu());
        assertEquals("1", deploy.getMemoryInGB());
        assertEquals("1", deploy.getInstanceCount());
        assertNull(deploy.getJvmOptions());
    }

    @Test
    public void testChildProjectAdvanced() throws Exception {
        final PluginParameterExpressionEvaluator pluginParameterExpressionEvaluator = mock(PluginParameterExpressionEvaluator.class);
        when(mockEval.evaluate("${evalBad}")).thenReturn("bad-text!!!!");
        when(mockEval.evaluate("${eval}")).thenReturn("appname");
        whenNew(PluginParameterExpressionEvaluator.class).withAnyArguments().thenReturn(pluginParameterExpressionEvaluator);
        mojo.project = TestHelper.createChildProject("service");

        final SpringServiceClient mockServiceClient = mock(SpringServiceClient.class);
        final List<ServiceResourceInner> serviceList = TestHelper.createServiceList();
        when(mockServiceClient.getAvailableClusters()).thenReturn(serviceList);
        whenNew(SpringServiceClient.class).withAnyArguments().thenReturn(mockServiceClient);
        TestHelper.mockAzureWithSubs(TestHelper.createMockSubscriptions(2));
        // 1. for subs
        // 2. cluster
        // 3. appName
        // 4. isPublic

        when(reader.readLine()).thenReturn("1").thenReturn("2")
            .thenReturn("$$%#").thenReturn("${evalBad}").thenReturn("${eval}").thenReturn("y").thenReturn("2").thenReturn("");
        FieldUtils.writeField(mojo, "advancedOptions", true, true);
        mojo.isTelemetryAllowed = false;
        initMockPromptWrapper();
        mojo.execute();
        final AppSettings app = (AppSettings) FieldUtils.readField(mojo, "appSettings", true);
        final DeploymentSettings deploy = (DeploymentSettings) FieldUtils.readField(mojo, "deploymentSettings", true);
        assertEquals("new_subs_id", app.getSubscriptionId());
        assertEquals("testCluster2", app.getClusterName());
        assertEquals("${eval}", app.getAppName());
        assertEquals("true", app.isPublic());
        assertEquals("1", deploy.getCpu());
        assertEquals("1", deploy.getMemoryInGB());
        assertEquals("2", deploy.getInstanceCount());
        assertNull(deploy.getJvmOptions());
    }

    private void initializeParentChildModel() throws Exception {
        final PluginParameterExpressionEvaluator pluginParameterExpressionEvaluator = mock(PluginParameterExpressionEvaluator.class);
        whenNew(PluginParameterExpressionEvaluator.class).withAnyArguments().thenReturn(pluginParameterExpressionEvaluator);
        when(project.getPackaging()).thenReturn("pom");
        mojo.isTelemetryAllowed = false;
        final List<MavenProject> parentChildProjects = TestHelper.prepareParentChildProjects();
        when(session.getAllProjects()).thenReturn(parentChildProjects);
        initMockPromptWrapper();
    }

    /**
     * @throws IOException
     * @throws InvalidConfigurationException
     * @throws IllegalAccessException
     * @throws Exception
     */
    private void initMockPromptWrapper() throws IOException, InvalidConfigurationException, IllegalAccessException, Exception {
        final PromptWrapper wrapper = new PromptWrapper(mockEval, mockLog) {
            private boolean initialized = false;
            public void initialize() throws IOException, InvalidConfigurationException {
                if (!initialized) {
                    initialized = true;
                    super.initialize();
                }
            }
        };
        wrapper.initialize();
        final Object prompt = FieldUtils.readField(wrapper, "prompt", true);
        FieldUtils.writeField(prompt, "reader", reader, true);

        whenNew(PromptWrapper.class).withAnyArguments().thenReturn(wrapper);
    }
}
