/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.model;

import lombok.Getter;

@Getter
public enum SpringCloudDeploymentStatus {
    UNKNOWN("Unknown"),
    STOPPED("Stopped"),
    RUNNING("Running"),
    FAILED("Failed"),
    ALLOCATING("Allocating"),
    UPGRADING("Upgrading"),
    COMPILING("Compiling");
    private final String label;

    SpringCloudDeploymentStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
