/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.DeploymentResourceStatus;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class Utils {
    private static final int POLLING_INTERVAL = 1;

    protected static final List<String> DEPLOYMENT_PROCESSING_STATUS =
            Arrays.asList(DeploymentResourceStatus.COMPILING.toString(), DeploymentResourceStatus.ALLOCATING.toString(), DeploymentResourceStatus.UPGRADING.toString());

    public static boolean isDeploymentDone(SpringCloudDeployment deployment) {
        if (deployment == null) {
            return false;
        }
        final String deploymentResourceStatus = deployment.entity().getStatus();
        if (DEPLOYMENT_PROCESSING_STATUS.contains(deploymentResourceStatus)) {
            return false;
        }
        final String finalDiscoverStatus = BooleanUtils.isTrue(deployment.entity().isActive()) ? "UP" : "OUT_OF_SERVICE";
        final List<SpringCloudDeploymentInstanceEntity> instanceList = deployment.entity().getInstances();
        if (CollectionUtils.isEmpty(instanceList)) {
            return false;
        }
        final boolean isInstanceDeployed = instanceList.stream().noneMatch(instance ->
                StringUtils.equalsIgnoreCase(instance.status(), "waiting") || StringUtils.equalsIgnoreCase(instance.status(), "pending"));
        final boolean isInstanceDiscovered = instanceList.stream().allMatch(instance ->
                StringUtils.equalsIgnoreCase(instance.discoveryStatus(), finalDiscoverStatus));
        return isInstanceDeployed && isInstanceDiscovered;
    }

    /**
     * Get resource repeatedly until it match the predicate or timeout, will return null when meet exception
     * with default pollingInterval = 1s
     *
     * @param callable         callable to get resource
     * @param predicate        function that evaluate the resource
     * @param timeOutInSeconds max time for the method
     * @return the first resource which fit the predicate or the last result before timeout
     */
    public static <T> T pollUntil(Callable<T> callable, Predicate<T> predicate, int timeOutInSeconds) {
        return Utils.pollUntil(callable, predicate, timeOutInSeconds, POLLING_INTERVAL);
    }

    /**
     * Get resource repeatedly until it match the predicate or timeout, will return null when meet exception
     *
     * @param callable         callable to get resource
     * @param predicate        function that evaluate the resource
     * @param timeOutInSeconds max time for the method
     * @param pollingInterval  polling interval
     * @return the first resource which fit the predicate or the last result before timeout
     */
    public static <T> T pollUntil(Callable<T> callable, Predicate<T> predicate, int timeOutInSeconds, int pollingInterval) {
        final long timeout = System.currentTimeMillis() + timeOutInSeconds * 1000L;
        return Observable.interval(pollingInterval, TimeUnit.SECONDS)
                .timeout(timeOutInSeconds, TimeUnit.SECONDS)
                .flatMap(aLong -> Observable.fromCallable(callable))
                .subscribeOn(Schedulers.io())
                .takeUntil(resource -> predicate.test(resource) || System.currentTimeMillis() > timeout)
                .toBlocking().last();
    }
}
