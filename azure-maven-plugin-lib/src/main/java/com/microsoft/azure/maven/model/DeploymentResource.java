/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.model;

import org.apache.maven.model.Resource;

public class DeploymentResource extends Resource {

    private String type;
    private String targetDirectory;
    private Boolean clean;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public Boolean getClean() {
        return clean;
    }

    public void setClean(Boolean clean) {
        this.clean = clean;
    }

    @Override
    public String toString() {
        return "DeploymentResource{" + "type=" + type + ", targetDirectory=" + targetDirectory + ", clean=" + clean + ", " + super.toString() + '}';
    }
}
