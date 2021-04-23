/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.model.Resource;

public class DeploymentResource extends Resource {
    @Getter
    @Setter
    private String type;

    @Override
    public String toString() {
        return "DeploymentResource{" + "type=" + type + ", " + super.toString() + '}';
    }
}
