/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class DeploymentBuildStatus {

    public static final DeploymentBuildStatus TIMED_OUT = new DeploymentBuildStatus("TimedOut");
    public static final DeploymentBuildStatus RUNTIME_FAILED = new DeploymentBuildStatus("RuntimeFailed");
    public static final DeploymentBuildStatus BUILD_ABORTED = new DeploymentBuildStatus("BuildAborted");
    public static final DeploymentBuildStatus BUILD_FAILED = new DeploymentBuildStatus("BuildFailed");
    public static final DeploymentBuildStatus BUILD_REQUEST_RECEIVED = new DeploymentBuildStatus("BuildRequestReceived");
    public static final DeploymentBuildStatus BUILD_PENDING = new DeploymentBuildStatus("BuildPending");
    public static final DeploymentBuildStatus BUILD_IN_PROGRESS = new DeploymentBuildStatus("BuildInProgress");
    public static final DeploymentBuildStatus BUILD_SUCCESSFUL = new DeploymentBuildStatus("BuildSuccessful");
    public static final DeploymentBuildStatus POST_BUILD_RESTART_REQUIRED = new DeploymentBuildStatus("PostBuildRestartRequired");
    public static final DeploymentBuildStatus START_POLLING = new DeploymentBuildStatus("StartPolling");
    public static final DeploymentBuildStatus START_POLLING_WITH_RESTART = new DeploymentBuildStatus("StartPollingWithRestart");
    public static final DeploymentBuildStatus RUNTIME_STARTING = new DeploymentBuildStatus("RuntimeStarting");
    public static final DeploymentBuildStatus RUNTIME_SUCCESSFUL = new DeploymentBuildStatus("RuntimeSuccessful");

    private static final Set<DeploymentBuildStatus> SUCCEED_STATUS = Collections.unmodifiableSet(Sets.newHashSet(RUNTIME_SUCCESSFUL));
    private static final Set<DeploymentBuildStatus> FAILED_STATUS = Collections.unmodifiableSet(Sets.newHashSet(TIMED_OUT, RUNTIME_FAILED, BUILD_ABORTED, BUILD_FAILED));
    private static final Set<DeploymentBuildStatus> RUNNING_STATUS = Collections.unmodifiableSet(Sets.newHashSet(BUILD_REQUEST_RECEIVED,
            BUILD_PENDING, BUILD_IN_PROGRESS, BUILD_SUCCESSFUL, POST_BUILD_RESTART_REQUIRED, START_POLLING, START_POLLING_WITH_RESTART, RUNTIME_STARTING));
    private static final Set<DeploymentBuildStatus> VALUES = Stream.of(SUCCEED_STATUS, FAILED_STATUS, RUNNING_STATUS)
            .flatMap(Set::stream).collect(Collectors.toSet());

    private String value;

    public boolean isSucceed() {
        return SUCCEED_STATUS.contains(this);
    }

    public boolean isRunning() {
        return RUNNING_STATUS.contains(this);
    }

    public boolean isFailed() {
        return FAILED_STATUS.contains(this);
    }

    public boolean isUnknownStatus() {
        return !VALUES.contains(this);
    }

    public static DeploymentBuildStatus fromString(@NonNull final String value) {
        return VALUES.stream().filter(status -> StringUtils.equals(value, status.getValue()))
                .findFirst().orElseGet(() -> new DeploymentBuildStatus(value));
    }
}
