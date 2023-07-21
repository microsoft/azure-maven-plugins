/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Stream;

@Data
@Builder
@EqualsAndHashCode
public class CsmDeploymentStatus {
    private String deploymentId;
    private DeploymentBuildStatus status;
    private Integer numberOfInstancesInProgress;
    private Integer numberOfInstancesSuccessful;
    private Integer numberOfInstancesFailed;
    private List<String> failedInstancesLogs;
    private List<ErrorEntity> errors;

    public int getTotalInstanceCount() {
        return Stream.of(numberOfInstancesInProgress, numberOfInstancesFailed, numberOfInstancesSuccessful)
            .filter(i -> i != null)
            .mapToInt(Integer::intValue).sum();
    }
}
