/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public class RuntimeConfig {
    private Runtime runtime;
    private String image;
    private String registryUrl;
    private String username;
    private String password;
    private String startUpCommand;

    public OperatingSystem os() {
        if (Objects.isNull(runtime)) {
            return null;
        }
        return runtime.getOperatingSystem();
    }
}
