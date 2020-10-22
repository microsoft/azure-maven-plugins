/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.model;

import org.apache.maven.model.Resource;

public class DeploymentResource extends Resource {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "DeployResource{type='" + type + ',' + super.toString() + '}';
    }
}
