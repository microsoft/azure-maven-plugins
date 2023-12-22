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
public class FunctionAppDockerRuntime implements FunctionAppRuntime {
    public static FunctionAppDockerRuntime INSTANCE = new FunctionAppDockerRuntime();
    @Getter
    private final OperatingSystem operatingSystem = OperatingSystem.DOCKER;

    @Nonnull
    @Override
    public String getJavaVersionNumber() {
        return "null";
    }
}
