package com.microsoft.azure.maven.containerapps.parser;

import com.microsoft.azure.maven.containerapps.AbstractMojoBase;
import com.microsoft.azure.maven.containerapps.config.AppContainerMavenConfig;
import com.microsoft.azure.maven.containerapps.config.DeploymentType;
import com.microsoft.azure.maven.containerapps.config.IngressMavenConfig;
import com.microsoft.azure.maven.utils.MavenUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.containerapps.config.ContainerAppConfig;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.ResourceConfiguration;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistryModule;
import com.microsoft.azure.toolkit.lib.containerregistry.config.ContainerRegistryConfig;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ConfigParserTest {

    @Mock
    private AbstractMojoBase mojo;

    @Mock
    private MavenProject project;

    @Mock
    private AzureContainerRegistryModule mockRegistryModule;

    @Mock
    private AzureContainerRegistry mockAzureContainerRegistry;

    private ConfigParser configParser;
    private MockedStatic<Azure> azureMockedStatic;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mojo.getProject()).thenReturn(project);
        configParser = new ConfigParser(mojo);
        mockCommonMethods();
    }

    @After
    public void tearDown() {
        if (azureMockedStatic != null) {
            azureMockedStatic.close();
        }
    }

    private void mockCommonMethods() {
        when(mojo.getSubscriptionId()).thenReturn("subscription-id");
        when(mojo.getResourceGroup()).thenReturn("resource-group");
        when(mojo.getAppEnvironmentName()).thenReturn("app-environment");
        when(mojo.getRegion()).thenReturn("region");
        when(mojo.getAppName()).thenReturn("app-name");

        // Mock Azure.az to return mockAzureContainerRegistry
        azureMockedStatic = mockStatic(Azure.class);
        azureMockedStatic.when(() -> Azure.az(AzureContainerRegistry.class)).thenReturn(mockAzureContainerRegistry);
        when(mockAzureContainerRegistry.registry(anyString())).thenReturn(mockRegistryModule);
        when(mockRegistryModule.listByResourceGroup(anyString())).thenReturn(Collections.emptyList());
    }

    @Test
    public void testGetContainerAppConfigImage() {
        AppContainerMavenConfig containerConfig = new AppContainerMavenConfig();
        containerConfig.setCpu(1.0);
        containerConfig.setMemory("2Gi");
        containerConfig.setImage("my-image:latest");
        containerConfig.setEnvironment(Collections.emptyList());
        when(mojo.getContainers()).thenReturn(Collections.singletonList(containerConfig));

        IngressMavenConfig ingressConfig = new IngressMavenConfig();
        ingressConfig.setExternal(true);
        ingressConfig.setTargetPort(80);
        when(mojo.getIngress()).thenReturn(ingressConfig);

        ContainerAppDraft.ScaleConfig scaleConfig = ContainerAppDraft.ScaleConfig.builder().minReplicas(1).maxReplicas(3).build();
        when(mojo.getScale()).thenReturn(scaleConfig);

        ContainerAppConfig config = configParser.getContainerAppConfig();

        // Assertions
        assertNotNull(config);
        assertEquals("subscription-id", config.getEnvironment().getSubscriptionId());
        assertEquals("resource-group", config.getEnvironment().getResourceGroup());
        assertEquals("app-environment", config.getEnvironment().getAppEnvironmentName());
        assertEquals("region", config.getEnvironment().getRegion());
        assertEquals("app-name", config.getAppName());

        ResourceConfiguration resourceConfig = config.getResourceConfiguration();
        assertNotNull(resourceConfig);
        assertEquals(1.0, resourceConfig.getCpu(), 0);
        assertEquals("2Gi", resourceConfig.getMemory());

        IngressConfig ingress = config.getIngressConfig();
        assertNotNull(ingress);
        assertTrue(ingress.isEnableIngress());
        assertTrue(ingress.isExternal());
        assertEquals(80, ingress.getTargetPort());

        ContainerAppDraft.ScaleConfig scale = config.getScaleConfig();
        assertNotNull(scale);
        assertEquals(1, (int) scale.getMinReplicas());
        assertEquals(3, (int) scale.getMaxReplicas());

        ContainerAppDraft.ImageConfig imageConfig = config.getImageConfig();
        assertNotNull(imageConfig);
        assertEquals("my-image:latest", imageConfig.getFullImageName());

        ContainerRegistryConfig registryConfig = config.getRegistryConfig();
        assertNotNull(registryConfig);
        // check registry config name starts with acr when no registry is provided
        assertTrue(registryConfig.getRegistryName().startsWith("acr"));
    }

    @Test
    public void testGetContainerAppConfigCode() {
        try (MockedStatic<MavenUtils> mavenUtilsMockedStatic = mockStatic(MavenUtils.class)) {
            mavenUtilsMockedStatic.when(() -> MavenUtils.isSpringBootProject(any())).thenReturn(true);

            AppContainerMavenConfig containerConfig = new AppContainerMavenConfig();
            containerConfig.setType(DeploymentType.CODE.name());
            containerConfig.setDirectory(".");
            containerConfig.setEnvironment(Collections.emptyList());
            when(mojo.getContainers()).thenReturn(Collections.singletonList(containerConfig));

            ContainerAppConfig config = configParser.getContainerAppConfig();

            ContainerAppDraft.ImageConfig imageConfig = config.getImageConfig();
            assertNotNull(imageConfig);
            assertTrue(imageConfig.getFullImageName().contains("/app-name:"));
        }
    }
}
