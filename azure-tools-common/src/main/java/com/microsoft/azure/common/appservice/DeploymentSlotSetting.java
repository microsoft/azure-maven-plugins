/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.appservice;

/**
 * Deployment Slot setting class.
 */
public class DeploymentSlotSetting {
    protected String name;
    protected String configurationSource;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfigurationSource() {
        return this.configurationSource;
    }

    public void setConfigurationSource(String configurationSource) {
        this.configurationSource = configurationSource;
    }
}
