/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

/**
 * Deployment Slot setting class.
 */
public class DeploymentSlotSetting {
    protected String slotName;
    protected String configurationSource;

    public String getSlotName() {
        return this.slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getConfigurationSource() {
        return this.configurationSource;
    }

    public void setConfigurationSource(String configurationSource) {
        this.configurationSource = configurationSource;
    }
}
