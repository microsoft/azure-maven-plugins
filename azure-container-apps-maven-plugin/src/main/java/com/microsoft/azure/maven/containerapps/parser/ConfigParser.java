package com.microsoft.azure.maven.containerapps.parser;

import com.microsoft.azure.maven.containerapps.AbstractMojoBase;
import com.microsoft.azure.maven.containerapps.config.AppContainerMavenConfig;
import com.microsoft.azure.maven.containerapps.config.DeploymentType;
import com.microsoft.azure.maven.containerapps.config.IngressMavenConfig;
import com.microsoft.azure.maven.utils.MavenUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerapps.config.ContainerAppConfig;
import com.microsoft.azure.toolkit.lib.containerapps.config.ContainerAppsEnvironmentConfig;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.ResourceConfiguration;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.config.ContainerRegistryConfig;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConfigParser {

    protected AbstractMojoBase mojo;

    private final String timestamp;

    public ConfigParser(AbstractMojoBase mojo) {
        this.mojo = mojo;
        this.timestamp = Utils.getTimestamp();
    }

    public ContainerAppConfig getContainerAppConfig() {
        final ContainerAppConfig config = new ContainerAppConfig();
        final ContainerAppsEnvironmentConfig envConfig = new ContainerAppsEnvironmentConfig();
        envConfig.setSubscriptionId(mojo.getSubscriptionId());
        envConfig.setResourceGroup(mojo.getResourceGroup());
        envConfig.setAppEnvironmentName(mojo.getAppEnvironmentName());
        envConfig.setRegion(mojo.getRegion());
        config.setEnvironment(envConfig);
        config.setAppName(mojo.getAppName());
        config.setResourceConfiguration(getResourceConfigurationFromContainers(mojo.getContainers()));
        config.setIngressConfig(getIngressConfig(mojo.getIngress()));
        config.setRegistryConfig(getRegistryConfig());
        config.setImageConfig(getImageConfigFromContainers(config));
        config.setScaleConfig(mojo.getScale());
        return config;
    }

    public ContainerAppDraft.ImageConfig getImageConfigFromContainers(ContainerAppConfig config) {
        List<AppContainerMavenConfig> containers = mojo.getContainers();
        if (containers == null || containers.isEmpty()) {
            return null;
        }
        final String defaultImageName = String.format("%s%s/%s:%s", config.getRegistryConfig().getRegistryName(), ContainerRegistry.ACR_IMAGE_SUFFIX, mojo.getAppName(), timestamp);
        final String fullImageName = Optional.ofNullable(containers.get(0).getImage()).orElse(defaultImageName);
        final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(fullImageName);
        if (containers.get(0).getEnvironment() != null) {
            imageConfig.setEnvironmentVariables(containers.get(0).getEnvironment());
        }
        if (containers.get(0).getDeploymentType() == DeploymentType.CODE || containers.get(0).getDeploymentType() == DeploymentType.ARTIFACT) {
            ContainerAppDraft.BuildImageConfig buildImageConfig = new ContainerAppDraft.BuildImageConfig();
            buildImageConfig.setSource(Paths.get(containers.get(0).getDirectory()));
            //Check if we can generate dockerfile for this project. Currently only support spring boot project
            if (!imageConfig.sourceHasDockerFile()) {
                if (!MavenUtils.isSpringBootProject(mojo.getProject())) {
                    throw new AzureToolkitRuntimeException("Cannot generate Dockerfile for non-spring-boot project");
                }
            }
            //detect java version
            Map<String, String> sourceBuildEnv = new HashMap<>();
            String javaVersion = MavenUtils.getJavaVersion(mojo.getProject());
            if (StringUtils.isNotEmpty(javaVersion)) {
                sourceBuildEnv.put("JAVA_VERSION", javaVersion);
            }
            buildImageConfig.setSourceBuildEnv(sourceBuildEnv);
            imageConfig.setBuildImageConfig(buildImageConfig);
        }
        if (!StringUtils.isEmpty(mojo.getIdentity())) {
            imageConfig.setIdentity(mojo.getIdentity());
        }

        return imageConfig;
    }

    public ResourceConfiguration getResourceConfigurationFromContainers(List<AppContainerMavenConfig> containers) {
        if (containers == null || containers.isEmpty()) {
            return null;
        }
        if (containers.get(0).getCpu() == null && containers.get(0).getMemory() == null) {
            return null;
        }
        final ResourceConfiguration resourceConfiguration = new ResourceConfiguration();
        resourceConfiguration.setCpu(containers.get(0).getCpu());
        resourceConfiguration.setMemory(containers.get(0).getMemory());
        return resourceConfiguration;
    }

    public IngressConfig getIngressConfig(IngressMavenConfig ingressMavenConfig) {
        if (ingressMavenConfig == null) {
            return null;
        }
        IngressConfig ingressConfig = new IngressConfig();
        ingressConfig.setEnableIngress(true);
        ingressConfig.setExternal(ingressMavenConfig.getExternal());
        ingressConfig.setTargetPort(ingressMavenConfig.getTargetPort());
        return ingressConfig;
    }

    public ContainerRegistryConfig getRegistryConfig() {
        final String defaultRegistryName = String.format("acr%s", timestamp);
        ContainerRegistryConfig config = new ContainerRegistryConfig();
        if (mojo.getRegistry() == null || mojo.getRegistry().getRegistryName() == null) {
            final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).registry(mojo.getSubscriptionId())
                .listByResourceGroup(mojo.getResourceGroup()).stream().filter(ContainerRegistry::isAdminUserEnabled).findFirst().orElse(null);
            config.setRegistryName(Optional.ofNullable(registry).map(ContainerRegistry::getName).orElse(defaultRegistryName));
        }
        else {
            config.setRegistryName(mojo.getRegistry().getRegistryName());
        }
        config.setResourceGroup(Optional.ofNullable(mojo.getRegistry()).map(ContainerRegistryConfig::getResourceGroup).orElse(mojo.getResourceGroup()));
        config.setSubscriptionId(Optional.ofNullable(mojo.getRegistry()).map(ContainerRegistryConfig::getSubscriptionId).orElse(mojo.getSubscriptionId()));
        config.setRegion(Optional.ofNullable(mojo.getRegistry()).map(ContainerRegistryConfig::getRegion).orElse(mojo.getRegion()));
        return config;
    }
}
