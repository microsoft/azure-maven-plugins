/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.springcloud;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceProperties;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceProvisioningState;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentSettings;
import com.microsoft.azure.management.appplatform.v2020_07_01.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceInfo;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceType;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentsInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.SkuInner;
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

public class AppDeploymentClient extends AbstractClientBase {
    private static final String RUNTIME_VERSION_PATTERN = "(J|j)ava((\\s)?|_)(8|11)$";
    private static final RuntimeVersion DEFAULT_RUNTIME_VERSION = RuntimeVersion.JAVA_8;
    private static final int SCALING_TIME_OUT = 60; // Use same timeout as service

    private final String appName;
    private final String deploymentName;
    private final AppClient cloudAppClient;

    public static class Builder extends AbstractClientBase.Builder<Builder> {
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

        public AppDeploymentClient build() {
            return new AppDeploymentClient(this);
        }

        public Builder self() {
            return this;
        }
    }

    public AppDeploymentClient(Builder builder) {
        super(builder);
        this.appName = builder.appName;
        this.deploymentName = builder.deploymentName;
        this.cloudAppClient = springServiceClient.newSpringAppClient(subscriptionId, clusterName, appName);
    }

    public AppDeploymentClient(AppClient cloudAppClient, String deploymentName) {
        super(cloudAppClient);
        this.appName = cloudAppClient.appName;
        this.deploymentName = deploymentName;
        this.cloudAppClient = cloudAppClient;
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

    public DeploymentResourceInner createDeployment(AppDeploymentConfig deploymentConfig, ResourceUploadDefinitionInner resource) {
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties();
        final SkuInner skuInner = this.initDeploymentSku(deploymentConfig);
        final DeploymentSettings deploymentSettings = new DeploymentSettings();
        final RuntimeVersion runtimeVersion = getRuntimeVersion(deploymentConfig.getRuntimeVersion(), null);
        deploymentSettings.withCpu(deploymentConfig.getCpu())
                .withJvmOptions(deploymentConfig.getJvmOptions())
                .withMemoryInGB(deploymentConfig.getMemoryInGB())
                .withRuntimeVersion(runtimeVersion)
                .withEnvironmentVariables(deploymentConfig.getEnvironment());
        final UserSourceInfo userSourceInfo = new UserSourceInfo();
        userSourceInfo.withType(UserSourceType.JAR).withRelativePath(resource.relativePath());
        deploymentProperties.withSource(userSourceInfo).withDeploymentSettings(deploymentSettings);

        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner();
        tempDeploymentResource.withSku(skuInner).withProperties(deploymentProperties);
        // Create deployment
        final DeploymentsInner inner = springManager.deployments().inner();
        final DeploymentResourceInner deployment = inner.createOrUpdate(resourceGroup, clusterName, appName, deploymentName, tempDeploymentResource);
        inner.start(resourceGroup, clusterName, appName, deploymentName);
        return deployment;
    }

    public DeploymentResourceInner updateDeployment(DeploymentResourceInner deployment, AppDeploymentConfig deploymentConfig,
                                                    ResourceUploadDefinitionInner resource) throws AzureExecutionException {
        final DeploymentSettings previousDeploymentSettings = deployment.properties().deploymentSettings();
        if (isResourceScaled(deploymentConfig, deployment)) {
            Log.info("Scaling deployment...");
            scaleDeployment(deploymentConfig);
            Log.info("Scaling deployment done.");
        }
        final RuntimeVersion runtimeVersion = getRuntimeVersion(deploymentConfig.getRuntimeVersion(), previousDeploymentSettings.runtimeVersion());
        // Update deployment configuration, scale related parameters should be update in scaleDeployment()
        final DeploymentSettings newDeploymentSettings = new DeploymentSettings()
                .withJvmOptions(deploymentConfig.getJvmOptions())
                .withRuntimeVersion(runtimeVersion)
                .withEnvironmentVariables(deploymentConfig.getEnvironment());
        final UserSourceInfo userSourceInfo = new UserSourceInfo()
                .withType(UserSourceType.JAR)
                .withRelativePath(resource.relativePath());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties()
                .withSource(userSourceInfo)
                .withDeploymentSettings(newDeploymentSettings);
        deployment
                .withProperties(deploymentProperties)
                .withSku(null); // server cannot update and scale deployment at the same time
        final DeploymentResourceInner result = springManager.deployments().inner().update(resourceGroup, clusterName,
                appName, deploymentName, deployment);
        springManager.deployments().inner().start(resourceGroup, clusterName, appName, deploymentName);
        return result;
    }

    private DeploymentResourceInner scaleDeployment(AppDeploymentConfig deploymentConfig) throws AzureExecutionException {
        final SkuInner skuInner = this.initDeploymentSku(deploymentConfig);
        final DeploymentSettings deploymentSettings = new DeploymentSettings()
                .withCpu(deploymentConfig.getCpu())
                .withMemoryInGB(deploymentConfig.getMemoryInGB());
        final DeploymentResourceProperties deploymentProperties = new DeploymentResourceProperties()
                .withDeploymentSettings(deploymentSettings);
        final DeploymentResourceInner tempDeploymentResource = new DeploymentResourceInner()
                .withSku(skuInner)
                .withProperties(deploymentProperties);

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

    private SkuInner initDeploymentSku(AppDeploymentConfig deploymentConfig) {
        final SkuInner clusterSku = this.cluster.sku();
        return new SkuInner().withName(clusterSku.name())
                .withTier(clusterSku.tier())
                .withCapacity(deploymentConfig.getInstanceCount());
    }

    private static boolean isResourceScaled(AppDeploymentConfig deploymentConfig, DeploymentResourceInner deployment) {
        final DeploymentSettings deploymentSettings = deployment.properties().deploymentSettings();
        return !(Objects.equals(deploymentConfig.getCpu(), deploymentSettings.cpu()) &&
                Objects.equals(deploymentConfig.getMemoryInGB(), deploymentSettings.memoryInGB()) &&
                Objects.nonNull(deployment.sku()) &&
                Objects.equals(deploymentConfig.getInstanceCount(), deployment.sku().capacity()));
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
