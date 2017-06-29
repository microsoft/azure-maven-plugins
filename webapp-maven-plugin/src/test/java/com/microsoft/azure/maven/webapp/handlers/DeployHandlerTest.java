/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApps;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.maven.webapp.handlers.ContainerDeployHandler.CONTAINER_NOT_SUPPORTED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployHandlerTest {
    private static final String IMAGE_NAME = "ImageName";
    private static final String START_UP_FILE = "Command";
    private static final String SERVER_ID = "ServerId";
    private static final String PRIVATE_REGISTRY_URL = "https://www.microsoft.com";
    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";
    private static final String RESOURCE_GROUP = "ResourceGroupName";
    private static final String APP_NAME = "WebAppName";
    private static final String REGION = "Region";
    private static final String PORT = "PORT";
    private static final String PORT_NUMBER = "80";

    @Mock
    private Settings settings;

    @Mock
    private Azure azure;

    @Mock
    private ResourceGroups groups;

    @Mock
    private WebApps webApps;

    @Mock
    private WebApp.DefinitionStages.Blank blank;

    @Mock
    private WebApp.DefinitionStages.NewAppServicePlanWithGroup newPlanWithGroup;

    @Mock
    private WebApp.DefinitionStages.WithNewAppServicePlan newPlan1;

    @Mock
    private WebApp.DefinitionStages.WithNewAppServicePlan newPlan2;

    @Mock
    private WebApp.DefinitionStages.WithDockerContainerImage dockerWithNewGroup;

    @Mock
    private WebApp.DefinitionStages.WithDockerContainerImage dockerWithExistingGroup;

    @Mock
    private WebApp.DefinitionStages.WithStartUpCommand startUpCommand1;

    @Mock
    private WebApp.DefinitionStages.WithCredentials credential1;

    @Mock
    private WebApp.DefinitionStages.WithCreate create;

    @Mock
    private WebApp app;

    @Mock
    private SiteInner inner;

    @Mock
    private DeployMojo mojo;

    @Mock
    private Log log;

    @Mock
    private WebApp.Update update;

    @Mock
    private WebApp.UpdateStages.WithStartUpCommand startUpCommand2;

    @Mock
    private WebApp.UpdateStages.WithCredentials credential2;

    private Map<String, String> appSettings;

    private void setUpDeployMojo() throws Exception {
        appSettings = new HashMap<>();
        appSettings.put(PORT, PORT_NUMBER);

        final ContainerSetting container = new ContainerSetting();
        container.setImageName(IMAGE_NAME);
        container.setStartUpFile(START_UP_FILE);
        container.setServerId(SERVER_ID);
        container.setRegistryUrl(new URL(PRIVATE_REGISTRY_URL));

        final Server server = new Server();
        server.setUsername(USERNAME);
        server.setPassword(PASSWORD);

        when(mojo.getRegion()).thenReturn(REGION);
        when(mojo.getResourceGroup()).thenReturn(RESOURCE_GROUP);
        when(mojo.getAppName()).thenReturn(APP_NAME);
        when(mojo.getPricingTier()).thenReturn(PricingTier.STANDARD_S1);
        when(mojo.getAppSettings()).thenReturn(appSettings);
        when(mojo.getContainerSetting()).thenReturn(container);
        when(mojo.getSettings()).thenReturn(settings);
        when(settings.getServer(SERVER_ID)).thenReturn(server);
        when(mojo.getAzureClient()).thenReturn(azure);
        when(mojo.getLog()).thenReturn(log);
    }

    private void setUpMockForNewApp() {
        when(azure.resourceGroups()).thenReturn(groups);
        when(azure.webApps()).thenReturn(webApps);
        when(webApps.define(APP_NAME)).thenReturn(blank);
        when(blank.withRegion(REGION)).thenReturn(newPlanWithGroup);
        when(newPlanWithGroup.withNewResourceGroup(RESOURCE_GROUP)).thenReturn(newPlan1);
        when(newPlanWithGroup.withExistingResourceGroup(RESOURCE_GROUP)).thenReturn(newPlan2);
        when(newPlan1.withNewLinuxPlan(PricingTier.STANDARD_S1)).thenReturn(dockerWithNewGroup);
        when(newPlan2.withNewLinuxPlan(PricingTier.STANDARD_S1)).thenReturn(dockerWithExistingGroup);
        when(credential1.withCredentials(USERNAME, PASSWORD)).thenReturn(startUpCommand1);
        when(startUpCommand1.withStartUpCommand(START_UP_FILE)).thenReturn(create);
        when(create.withAppSettings(appSettings)).thenReturn(create);
    }

    private void setUpMockForExistingApp() {
        when(app.inner()).thenReturn(inner);
        when(app.update()).thenReturn(update);
        when(app.operatingSystem()).thenReturn(OperatingSystem.LINUX);
        when(credential2.withCredentials(USERNAME, PASSWORD)).thenReturn(startUpCommand2);
        when(startUpCommand2.withStartUpCommand(START_UP_FILE)).thenReturn(update);
        when(update.withAppSettings(appSettings)).thenReturn(update);
    }

    private void setUpMockForPublicDockerHubImage() {
        when(dockerWithNewGroup.withPublicDockerHubImage(IMAGE_NAME)).thenReturn(startUpCommand1);
        when(dockerWithExistingGroup.withPublicDockerHubImage(IMAGE_NAME)).thenReturn(startUpCommand1);
        when(update.withPublicDockerHubImage(IMAGE_NAME)).thenReturn(startUpCommand2);
    }

    private void setUpMockForPrivateDockerHubImage() {
        when(dockerWithNewGroup.withPrivateDockerHubImage(IMAGE_NAME)).thenReturn(credential1);
        when(dockerWithExistingGroup.withPrivateDockerHubImage(IMAGE_NAME)).thenReturn(credential1);
        when(update.withPrivateDockerHubImage(IMAGE_NAME)).thenReturn(credential2);
    }

    private void setUpMockForPrivateRegistryImage() {
        when(dockerWithNewGroup.withPrivateRegistryImage(IMAGE_NAME, PRIVATE_REGISTRY_URL))
                .thenReturn(credential1);
        when(dockerWithExistingGroup.withPrivateRegistryImage(IMAGE_NAME, PRIVATE_REGISTRY_URL))
                .thenReturn(credential1);
        when(update.withPrivateRegistryImage(IMAGE_NAME, PRIVATE_REGISTRY_URL)).thenReturn(credential2);
    }


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        setUpDeployMojo();
        setUpMockForNewApp();
        setUpMockForExistingApp();
        setUpMockForPublicDockerHubImage();
        setUpMockForPrivateDockerHubImage();
        setUpMockForPrivateRegistryImage();
    }

    @Test
    public void testValidationFail() throws Exception {
        final PublicDockerHubDeployHandler handler = new PublicDockerHubDeployHandler(mojo);

        final WebApp winApp = mock(WebApp.class);
        when(winApp.operatingSystem()).thenReturn(OperatingSystem.WINDOWS);

        try {
            handler.validate(winApp);
        } catch (Exception e) {
            assertEquals(CONTAINER_NOT_SUPPORTED, e.getMessage());
        }
    }

    @Test
    public void testDeployPublicDockerHubImageWithNewAppNewGroup() throws Exception {
        final PublicDockerHubDeployHandler handler = new PublicDockerHubDeployHandler(mojo);

        when(groups.checkExistence(RESOURCE_GROUP)).thenReturn(false);
        handler.validate(null);
        handler.deploy(null);

        verify(newPlanWithGroup, times(1)).withNewResourceGroup(RESOURCE_GROUP);
        verify(newPlanWithGroup, never()).withExistingResourceGroup(RESOURCE_GROUP);
        verify(newPlan1, times(1)).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(newPlan2, never()).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(dockerWithNewGroup, times(1)).withPublicDockerHubImage(IMAGE_NAME);
        verifyNoMoreInteractions(dockerWithExistingGroup);
        verifyNoMoreInteractions(update);
    }

    @Test
    public void testDeployPublicDockerHubImageWithNewAppExistingGroup() throws Exception {
        final PublicDockerHubDeployHandler handler = new PublicDockerHubDeployHandler(mojo);

        when(groups.checkExistence(RESOURCE_GROUP)).thenReturn(true);
        handler.validate(null);
        handler.deploy(null);

        verify(newPlanWithGroup, never()).withNewResourceGroup(RESOURCE_GROUP);
        verify(newPlanWithGroup, times(1)).withExistingResourceGroup(RESOURCE_GROUP);
        verify(newPlan1, never()).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(newPlan2, times(1)).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(dockerWithExistingGroup, times(1)).withPublicDockerHubImage(IMAGE_NAME);
        verifyNoMoreInteractions(dockerWithNewGroup);
        verifyNoMoreInteractions(update);
    }

    @Test
    public void testDeployPublicDockerHubImageWithExistingApp() throws Exception {
        final PublicDockerHubDeployHandler handler = new PublicDockerHubDeployHandler(mojo);

        handler.validate(app);
        handler.deploy(app);

        verify(webApps, never()).define(APP_NAME);
        verify(update, times(1)).withPublicDockerHubImage(IMAGE_NAME);
    }

    @Test
    public void testDeployPrivateDockerHubImageWithNewAppNewGroup() throws Exception {
        final PrivateDockerHubDeployHandler handler = new PrivateDockerHubDeployHandler(mojo);

        when(groups.checkExistence(RESOURCE_GROUP)).thenReturn(false);
        handler.validate(null);
        handler.deploy(null);

        verify(newPlanWithGroup, times(1)).withNewResourceGroup(RESOURCE_GROUP);
        verify(newPlanWithGroup, never()).withExistingResourceGroup(RESOURCE_GROUP);
        verify(newPlan1, times(1)).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(newPlan2, never()).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(dockerWithNewGroup, times(1)).withPrivateDockerHubImage(IMAGE_NAME);
        verify(credential1, times(1)).withCredentials(USERNAME, PASSWORD);
        verifyNoMoreInteractions(dockerWithExistingGroup);
        verifyNoMoreInteractions(update);
    }

    @Test
    public void testDeployPrivateDockerHubImageWithNewAppExistingGroup() throws Exception {
        final PrivateDockerHubDeployHandler handler = new PrivateDockerHubDeployHandler(mojo);

        when(groups.checkExistence(RESOURCE_GROUP)).thenReturn(true);
        handler.validate(null);
        handler.deploy(null);

        verify(newPlanWithGroup, never()).withNewResourceGroup(RESOURCE_GROUP);
        verify(newPlanWithGroup, times(1)).withExistingResourceGroup(RESOURCE_GROUP);
        verify(newPlan1, never()).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(newPlan2, times(1)).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(dockerWithExistingGroup, times(1)).withPrivateDockerHubImage(IMAGE_NAME);
        verify(credential1, times(1)).withCredentials(USERNAME, PASSWORD);
        verifyNoMoreInteractions(dockerWithNewGroup);
        verifyNoMoreInteractions(update);
    }

    @Test
    public void testDeployPrivateDockerHubImageWithExistingApp() throws Exception {
        final PrivateDockerHubDeployHandler handler = new PrivateDockerHubDeployHandler(mojo);

        handler.validate(app);
        handler.deploy(app);

        verify(webApps, never()).define(APP_NAME);
        verify(update, times(1)).withPrivateDockerHubImage(IMAGE_NAME);
        verify(credential2, times(1)).withCredentials(USERNAME, PASSWORD);
    }

    @Test
    public void testDeployPrivateRegistryImageWithNewAppNewGroup() throws Exception {
        final PrivateDockerRegistryDeployHandler handler = new PrivateDockerRegistryDeployHandler(mojo);

        when(groups.checkExistence(RESOURCE_GROUP)).thenReturn(false);
        handler.validate(null);
        handler.deploy(null);

        verify(newPlanWithGroup, times(1)).withNewResourceGroup(RESOURCE_GROUP);
        verify(newPlanWithGroup, never()).withExistingResourceGroup(RESOURCE_GROUP);
        verify(newPlan1, times(1)).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(newPlan2, never()).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(dockerWithNewGroup, times(1)).withPrivateRegistryImage(IMAGE_NAME, PRIVATE_REGISTRY_URL);
        verify(credential1, times(1)).withCredentials(USERNAME, PASSWORD);
        verifyNoMoreInteractions(dockerWithExistingGroup);
        verifyNoMoreInteractions(update);
    }

    @Test
    public void testDeployPrivateRegistryImageWithNewAppExistingGroup() throws Exception {
        final PrivateDockerRegistryDeployHandler handler = new PrivateDockerRegistryDeployHandler(mojo);

        when(groups.checkExistence(RESOURCE_GROUP)).thenReturn(true);
        handler.validate(null);
        handler.deploy(null);

        verify(newPlanWithGroup, never()).withNewResourceGroup(RESOURCE_GROUP);
        verify(newPlanWithGroup, times(1)).withExistingResourceGroup(RESOURCE_GROUP);
        verify(newPlan1, never()).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(newPlan2, times(1)).withNewLinuxPlan(PricingTier.STANDARD_S1);
        verify(dockerWithExistingGroup, times(1)).withPrivateRegistryImage(IMAGE_NAME, PRIVATE_REGISTRY_URL);
        verify(credential1, times(1)).withCredentials(USERNAME, PASSWORD);
        verifyNoMoreInteractions(dockerWithNewGroup);
        verifyNoMoreInteractions(update);
    }

    @Test
    public void testDeployPrivateRegistryImageWithExistingApp() throws Exception {
        final PrivateDockerRegistryDeployHandler handler = new PrivateDockerRegistryDeployHandler(mojo);

        handler.validate(app);
        handler.deploy(app);

        verify(webApps, never()).define(APP_NAME);
        verify(update, times(1)).withPrivateRegistryImage(IMAGE_NAME, PRIVATE_REGISTRY_URL);
        verify(credential2, times(1)).withCredentials(USERNAME, PASSWORD);
    }
}
