/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceProperties;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceProvisioningState;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentSettings;
import com.microsoft.azure.management.appplatform.v2020_07_01.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceInfo;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceType;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.SkuInner;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringDeploymentClient extends AbstractSpringClient {

    private static final String RUNTIME_VERSION_PATTERN = "(J|j)ava((\\s)?|_)(8|11)$";
    private static final RuntimeVersion DEFAULT_RUNTIME_VERSION = RuntimeVersion.JAVA_8;
    private static final int SCALING_TIME_OUT = 60; // Use same timeout as service

    private final String appName;
    private final String deploymentName;
    private final SpringAppClient springAppClient;

    public static class Builder extends AbstractSpringClient.Builder<Builder> {
        private String appName;
        private String deploymentName;

        public Builder withAppName(String appName) {
            this.appName = appName;
            return self();
        }

        public Builder withDeploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return self();
        }

        public SpringDeploymentClient build() {
            return new SpringDeploymentClient(this);
        }

        public Builder self() {
            return this;
        }
    }

    public SpringDeploymentClient(Builder builder) {
        super(builder);
        this.appName = builder.appName;
        this.deploymentName = builder.deploymentName;
        this.springAppClient = springServiceClient.newSpringAppClient(subscriptionId, clusterName, appName);
    }

    public SpringDeploymentClient(SpringAppClient springAppClient, String deploymentName) {
        super(springAppClient);
        this.appName = springAppClient.appName;
        this.deploymentName = deploymentName;
        this.springAppClient = springAppClient;
    }

    public DeploymentResourceInner createOrUpdateDeployment(Deployment configuration, ResourceUploadDefinitionInner resource) throws AzureExecutionException {
        final DeploymentResourceInner deployment = getDeployment();
        return deployment == null ? createDeployment(configuration, resource) :
                updateDeployment(configuration, deployment, resource);
    }

    public DeploymentResourceInner getDeployment() {
        return springManager.deployments().inner().get(resourceGroup, clusterName, appName, deploymentName);
    }

    public String getAppName() {
        return appName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    private DeploymentResourceInner createDeployment(Deployment deploymentConfiguration, ResourceUploadDefinitionInner resource) {
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();

        final SkuInner skuInner = new SkuInner();
        skuInner.withCapacity(deploymentConfiguration.getInstanceCount());

        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = getRuntimeVersion(deploymentConfiguration.getRuntimeVersion(), null);
        deploymentSettings.withCpu(deploymentConfiguration.getCpu())
                .withJvmOptions(deploymentConfiguration.getJvmOptions())
                .withMemoryInGB(deploymentConfiguration.getMemoryInGB())
                .withRuntimeVersion(runtimeVersion)
                .withEnvironmentVariables(deploymentConfiguration.getEnvironment());
        final UserSourceInfo userSourceInfo = new UserSourceInfo();
        userSourceInfo.withType(UserSourceType.JAR).withRelativePath(resource.relativePath());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(deploymentSettings);

        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner();
        tempDeploymentResource.withSku(skuInner).withProperties(deploymentProperties);
        // Create deployment
        final DeploymentResourceInner deployment = springManager.deployments().inner()
                .createOrUpdate(resourceGroup, clusterName, appName, deploymentName, tempDeploymentResource);
        springManager.deployments().inner().start(resourceGroup, clusterName, appName, deploymentName);
        // Active deployment
        if (StringUtils.isEmpty(springAppClient.getActiveDeploymentName())) {
            springAppClient.activateDeployment(deployment.name());
        }
        return deployment;
    }

    private DeploymentResourceInner updateDeployment(Deployment deploymentConfiguration, DeploymentResourceInner deployment,
                                                     ResourceUploadDefinitionInner resource) throws AzureExecutionException {
        final DeploymentSettings previousDeploymentSettings = deployment.properties().deploymentSettings();
        if (isResourceScaled(deploymentConfiguration, deployment)) {
            Log.info("Scaling deployment...");
            scaleDeployment(deploymentConfiguration);
            Log.info("Scaling deployment done.");
        }
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final DeploymentSettings newDeploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = getRuntimeVersion(deploymentConfiguration.getRuntimeVersion(), previousDeploymentSettings.runtimeVersion());
        // Update deployment configuration, scale related parameters should be update in scaleDeployment()
        newDeploymentSettings.withJvmOptions(deploymentConfiguration.getJvmOptions())
                .withRuntimeVersion(runtimeVersion)
                .withEnvironmentVariables(deploymentConfiguration.getEnvironment());
        final UserSourceInfo userSourceInfo = new UserSourceInfo();
        userSourceInfo.withType(UserSourceType.JAR).withRelativePath(resource.relativePath());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(newDeploymentSettings);
        final DeploymentResourceInner result = springManager.deployments().inner().update(resourceGroup, clusterName,
                appName, deploymentName, deployment);
        springManager.deployments().inner().start(resourceGroup, clusterName, appName, deploymentName);
        return result;
    }

    private DeploymentResourceInner scaleDeployment(Deployment deploymentConfiguration) throws AzureExecutionException {
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();

        final SkuInner skuInner = new SkuInner();
        skuInner.withCapacity(deploymentConfiguration.getInstanceCount());

        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        deploymentSettings.withCpu(deploymentConfiguration.getCpu())
                .withMemoryInGB(deploymentConfiguration.getMemoryInGB());
        deploymentProperties.withDeploymentSettings(deploymentSettings);

        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner();
        tempDeploymentResource.withSku(skuInner).withProperties(deploymentProperties);

        springManager.deployments().inner().update(resourceGroup, clusterName, appName, deploymentName, tempDeploymentResource);
        // Wait until deployment scaling done
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<DeploymentResourceInner> future = executor.submit(() -> {
            DeploymentResourceInner result = springManager.deployments().inner().get(resourceGroup, clusterName, appName, deploymentName);
            while (!isStableDeploymentResourceProvisioningState(result.properties().provisioningState())) {
                Thread.sleep(1000);
                result = springManager.deployments().inner().get(resourceGroup, clusterName, appName, deploymentName);
            }
            return result;
        });
        try {
            return future.get(SCALING_TIME_OUT, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new AzureExecutionException(String.format("Failed to scale deployment %s of spring cloud app %s", deploymentName, appName), e);
        }
    }

    private static boolean isResourceScaled(Deployment deploymentConfiguration, DeploymentResourceInner deployment) {
        final DeploymentSettings deploymentSettings = deployment.properties().deploymentSettings();
        return !(Objects.equals(deploymentConfiguration.getCpu(), deploymentSettings.cpu()) &&
                Objects.equals(deploymentConfiguration.getMemoryInGB(), deploymentSettings.memoryInGB()) &&
                Objects.nonNull(deployment.sku()) &&
                Objects.equals(deploymentConfiguration.getInstanceCount(), deployment.sku().capacity()));
    }

    private static boolean isStableDeploymentResourceProvisioningState(DeploymentResourceProvisioningState state) {
        return state == DeploymentResourceProvisioningState.SUCCEEDED || state == DeploymentResourceProvisioningState.FAILED;
    }

    private static RuntimeVersion getRuntimeVersion(String runtimeVersion, RuntimeVersion previousRuntimeVersion) {
        if (StringUtils.isAllEmpty(runtimeVersion)) {
            return previousRuntimeVersion == null ? DEFAULT_RUNTIME_VERSION : previousRuntimeVersion;
        }
        final String fixedRuntimeVersion = StringUtils.trim(runtimeVersion);
        final Matcher matcher = Pattern.compile(RUNTIME_VERSION_PATTERN).matcher(fixedRuntimeVersion);
        if (matcher.matches()) {
            return StringUtils.equals(matcher.group(4), "8") ? RuntimeVersion.JAVA_8 : RuntimeVersion.JAVA_11;
        } else {
            Log.warn(String.format("%s is not a valid runtime version, supported values are Java 8 and Java 11," +
                    " using Java 8 in this deployment.", fixedRuntimeVersion));
            return DEFAULT_RUNTIME_VERSION;
        }
    }
}
