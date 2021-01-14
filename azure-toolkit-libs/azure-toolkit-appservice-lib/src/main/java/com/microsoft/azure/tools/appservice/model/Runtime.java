/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.appservice.model;

import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class Runtime {
    private OperatingSystem operatingSystem;

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class Windows extends Runtime {
        private JavaVersion javaVersion;
        private WebContainer webContainer;
    }

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class Linux extends Runtime {
        private RuntimeStack runtimeStack;
    }

    @Getter
    @SuperBuilder(toBuilder = true)
    public static class Docker extends Runtime {
        private boolean isPublic;
        private String image;
        private String registryUrl;
        private String userName;
        private String password;
    }

    public static Runtime createFromServiceInstance(WebAppBase webApp) {
        return null;
    }
}
