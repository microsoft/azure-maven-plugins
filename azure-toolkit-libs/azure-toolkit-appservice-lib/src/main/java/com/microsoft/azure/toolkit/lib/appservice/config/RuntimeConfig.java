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
    private OperatingSystem os;
    /**
     * java version user input, e.g. Java 8, Java 11, Java 17, or 8, 11, 17, etc.
     */
    private String javaVersion;
    /**
     * web container user input, e.g. Tomcat 10.0, Tomcat 9.0, Java SE, etc.
     */
    private String webContainer;
    private String image;
    private String registryUrl;
    private String username;
    private String password;
    private String startUpCommand;
}
