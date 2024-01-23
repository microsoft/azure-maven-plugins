/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppWindowsRuntime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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

    public static RuntimeConfig fromRuntime(@Nonnull final Runtime runtime) {
        final RuntimeConfig result = new RuntimeConfig();
        result.os(runtime.getOperatingSystem());
        result.javaVersion(runtime.getJavaVersionUserText());
        Optional.of(runtime).filter(WebAppRuntime.class::isInstance)
            .map(WebAppRuntime.class::cast)
            .map(WebAppRuntime::getContainerUserText)
            .ifPresent(result::webContainer);
        return result;
    }

    @Nonnull
    public static Runtime toWebAppRuntime(@Nonnull RuntimeConfig runtime) {
        if (OperatingSystem.DOCKER != runtime.os()) {
            final WebAppRuntime webAppRuntime = runtime.os() == OperatingSystem.WINDOWS ?
                WebAppWindowsRuntime.fromContainerAndJavaVersionUserText(runtime.getWebContainer(), runtime.getJavaVersion()) :
                WebAppLinuxRuntime.fromContainerAndJavaVersionUserText(runtime.getWebContainer(), runtime.getJavaVersion());
            return Objects.requireNonNull(webAppRuntime, "Invalid runtime configuration.");
        } else {
            return WebAppDockerRuntime.INSTANCE;
        }
    }

    @Nonnull
    public static Runtime toFunctionAppRuntime(@Nonnull RuntimeConfig runtime) {
        if (OperatingSystem.DOCKER != runtime.os()) {
            final FunctionAppRuntime webAppRuntime = runtime.os() == OperatingSystem.WINDOWS ?
                FunctionAppWindowsRuntime.fromJavaVersionUserText(runtime.getJavaVersion()) :
                FunctionAppLinuxRuntime.fromJavaVersionUserText(runtime.getJavaVersion());
            return Objects.requireNonNull(webAppRuntime, "Invalid runtime configuration.");
        } else {
            return FunctionAppDockerRuntime.INSTANCE;
        }
    }

    public OperatingSystem getOs() {
        return os;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getWebContainer() {
        return webContainer;
    }

    public String getImage() {
        return image;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getStartUpCommand() {
        return startUpCommand;
    }

    public void setOs(OperatingSystem os) {
        this.os = os;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setWebContainer(String webContainer) {
        this.webContainer = webContainer;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStartUpCommand(String startUpCommand) {
        this.startUpCommand = startUpCommand;
    }
}
