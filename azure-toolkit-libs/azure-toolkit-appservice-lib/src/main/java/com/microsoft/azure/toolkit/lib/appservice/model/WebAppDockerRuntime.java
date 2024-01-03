/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebAppDockerRuntime implements WebAppRuntime {
    public static WebAppDockerRuntime INSTANCE = new WebAppDockerRuntime();
    @Getter
    private final OperatingSystem operatingSystem = OperatingSystem.DOCKER;

    @Nonnull
    @Override
    public String getContainerName() {
        return "DOCKER";
    }

    @Nonnull
    @Override
    public String getContainerVersionNumber() {
        return "null";
    }

    @Nonnull
    @Override
    public String getJavaVersionNumber() {
        return "null";
    }

    @Override
    public String toString() {
        return "Docker";
    }
}
