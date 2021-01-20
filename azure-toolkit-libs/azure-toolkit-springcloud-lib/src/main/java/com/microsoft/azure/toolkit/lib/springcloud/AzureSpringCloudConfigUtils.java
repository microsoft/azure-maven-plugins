/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appplatform.v2020_07_01.AppResourceProperties;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceProperties;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentSettings;
import com.microsoft.azure.management.appplatform.v2020_07_01.PersistentDisk;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceInfo;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceType;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.SkuInner;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureSpringCloudConfigUtils {
    private static final String DEFAULT_SKU_NAME = "S0";
    private static final String DEFAULT_SKU_TIER = "Standard";
    private static final String DEFAULT_DEPLOYMENT_NAME = "default";
    private static final String DEFAULT_ARTIFACT_RELATIVE_PATH = "<default>";
    private static final String DEFAULT_PERSISTENT_DISK_MOUNT_PATH = "/persistent";

    private static final int DEFAULT_PERSISTENT_DISK_SIZE = 50;
    private static final int DEFAULT_SKU_CAPACITY = 1;
    private static final int DEFAULT_MEMORY_IN_GB = 1;

    private static final String DEFAULT_RUNTIME_VERSION = SpringCloudJavaVersion.JAVA_8;

    private static final String RUNTIME_VERSION_PATTERN = "[Jj]ava((\\s)?|_)(8|11)$";
    private static final int TIMEOUT_SCALING = 60; // Use same timeout as service
    protected static final List<DeploymentResourceStatus> DEPLOYMENT_PROCESSING_STATUS =
        Arrays.asList(DeploymentResourceStatus.COMPILING, DeploymentResourceStatus.ALLOCATING, DeploymentResourceStatus.UPGRADING);

    public static PersistentDisk getPersistentDiskOrDefault(AppResourceProperties appResourceProperties) {
        final PersistentDisk disk = appResourceProperties.persistentDisk();
        return disk == null || disk.sizeInGB() <= 0 ? new PersistentDisk().withSizeInGB(DEFAULT_PERSISTENT_DISK_SIZE)
            .withMountPath(DEFAULT_PERSISTENT_DISK_MOUNT_PATH) : disk;
    }

    public static boolean isDeploymentDone(SpringCloudDeployment deployment) {
        if (deployment == null) {
            return false;
        }
        final DeploymentResourceStatus deploymentResourceStatus = deployment.entity().getStatus();
        if (DEPLOYMENT_PROCESSING_STATUS.contains(deploymentResourceStatus)) {
            return false;
        }
        final String finalDiscoverStatus = BooleanUtils.isTrue(deployment.entity().isActive()) ? "UP" : "OUT_OF_SERVICE";
        final List<DeploymentInstance> instanceList = deployment.entity().getInstances();
        if (CollectionUtils.isEmpty(instanceList)) {
            return false;
        }
        final boolean isInstanceDeployed = instanceList.stream().noneMatch(instance ->
            StringUtils.equalsIgnoreCase(instance.status(), "waiting") || StringUtils.equalsIgnoreCase(instance.status(), "pending"));
        final boolean isInstanceDiscovered = instanceList.stream().allMatch(instance ->
            StringUtils.equalsIgnoreCase(instance.discoveryStatus(), finalDiscoverStatus));
        return isInstanceDeployed && isInstanceDiscovered;
    }

    public static String normalize(String runtimeVersion) {
        if (StringUtils.isEmpty(runtimeVersion)) {
            return DEFAULT_RUNTIME_VERSION;
        }
        final String fixedRuntimeVersion = StringUtils.trim(runtimeVersion);
        final Matcher matcher = Pattern.compile(RUNTIME_VERSION_PATTERN).matcher(fixedRuntimeVersion);
        if (matcher.matches()) {
            return Objects.equals(matcher.group(3), "8") ? SpringCloudJavaVersion.JAVA_8 : SpringCloudJavaVersion.JAVA_11;
        } else {
            Log.warn(String.format("%s is not a valid runtime version, supported values are Java 8 and Java 11," +
                " using Java 8 in this deployment.", fixedRuntimeVersion));
            return DEFAULT_RUNTIME_VERSION;
        }
    }

    public static AppResourceProperties getOrCreateProperties(
        @Nonnull final AppResourceInner resource,
        @Nonnull final SpringCloudApp app
    ) {
        AppResourceProperties properties = resource.properties();
        if (Objects.isNull(properties)) {
            properties = new AppResourceProperties();
            resource.withProperties(properties);
        }
        return properties;
    }

    public static DeploymentResourceProperties getOrCreateProperties(
        @Nonnull final DeploymentResourceInner resource,
        @Nonnull final SpringCloudDeployment deployment
    ) {
        DeploymentResourceProperties properties = resource.properties();
        if (Objects.isNull(properties)) {
            properties = new DeploymentResourceProperties();
            resource.withProperties(properties);
        }
        return properties;
    }

    public static DeploymentSettings getOrCreateDeploymentSettings(
        @Nonnull final DeploymentResourceInner resource,
        @Nonnull final SpringCloudDeployment deployment
    ) {
        final DeploymentResourceProperties properties = getOrCreateProperties(resource, deployment);
        DeploymentSettings deploymentSettings = properties.deploymentSettings();
        if (Objects.isNull(deploymentSettings)) {
            deploymentSettings = new DeploymentSettings();
            properties.withDeploymentSettings(deploymentSettings);
        }
        return deploymentSettings;
    }

    public static SkuInner getOrCreateSku(
        @Nonnull final DeploymentResourceInner resource,
        @Nonnull final SpringCloudDeployment deployment
    ) {
        SkuInner sku = resource.sku();
        if (Objects.isNull(sku)) {
            final SkuInner clusterSku = deployment.getApp().getCluster().entity().getSku();
            sku = new SkuInner().withCapacity(DEFAULT_SKU_CAPACITY).withTier(clusterSku.tier()).withName(clusterSku.name());
            resource.withSku(sku);
        }
        return sku;
    }

    public static UserSourceInfo getOrCreateSource(
        @Nonnull final DeploymentResourceInner resource,
        @Nonnull final SpringCloudDeployment deployment
    ) {
        final DeploymentResourceProperties properties = getOrCreateProperties(resource, deployment);
        UserSourceInfo source = properties.source();
        if (Objects.isNull(source)) {
            source = new UserSourceInfo().withType(UserSourceType.JAR).withRelativePath(DEFAULT_ARTIFACT_RELATIVE_PATH);
            properties.withSource(source);
        }
        return source;
    }
}
