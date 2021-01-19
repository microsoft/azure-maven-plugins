/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder(toBuilder = true)
public class Runtime {
    private OperatingSystem operatingSystem;

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class Windows extends Runtime {
        private JavaVersion javaVersion;
        private WebContainer webContainer;

        public static Runtime createFromServiceInstance(WebAppBase webApp) {
            return Runtime.Windows.builder()
                    .operatingSystem(OperatingSystem.WINDOWS)
                    .javaVersion(JavaVersion.createFromServiceModel(webApp.javaVersion()))
                    .webContainer(WebContainer.getWebContainer(webApp.javaContainer(), webApp.javaContainerVersion()))
                    .build();
        }
    }

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class Linux extends Runtime {
        private RuntimeStack runtimeStack;

        public static Runtime createFromServiceInstance(WebAppBase webApp) {
            final String linuxFxVersion = webApp.linuxFxVersion();
            return Runtime.Linux.builder()
                    .operatingSystem(OperatingSystem.LINUX)
                    .runtimeStack(RuntimeStack.getFromLinuxFxVersion(linuxFxVersion))
                    .build();
        }
    }

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class Docker extends Runtime {
        private boolean isPublic;
        private String image;
        private String registryUrl;
        private String userName;
        private String password;

        public static Runtime createFromServiceInstance(WebAppBase webApp) {
            return null;
        }
    }

    public static Runtime createFromServiceInstance(WebAppBase webApp) {
        if (webApp.operatingSystem() == com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS) {
            return Runtime.Windows.createFromServiceInstance(webApp);
        }
        if (StringUtils.startsWithIgnoreCase(webApp.linuxFxVersion(), "Docker")) {
            return Runtime.Docker.createFromServiceInstance(webApp);
        }
        return Runtime.Linux.createFromServiceInstance(webApp);
    }
}
