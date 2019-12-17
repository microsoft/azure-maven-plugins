/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.microsoft.azure.maven.appservice.OperatingSystemEnum;

public abstract class RuntimeConfiguration {

    public static final OperatingSystemEnum DEFAULT_OS = OperatingSystemEnum.Windows;

    protected String os;
    protected String image;
    protected String registryUrl;

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }
}
