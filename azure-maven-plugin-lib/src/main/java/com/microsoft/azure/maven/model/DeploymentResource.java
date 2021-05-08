/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DeploymentResource extends Resource {
    private static final Path FTP_ROOT = Paths.get("/site/wwwroot");

    @Getter
    @Setter
    private String type;

    public boolean isExternalResource() {
        if (isOneDeployResource()) {
            return false;
        }
        final Path target = Paths.get(getAbsoluteTargetPath());
        return !target.startsWith(FTP_ROOT);
    }

    public String getAbsoluteTargetPath() {
        // convert null to empty string
        final String targetPath = StringUtils.defaultString(this.getTargetPath());
        return StringUtils.startsWith(targetPath, "/") ? targetPath :
                FTP_ROOT.resolve(Paths.get(targetPath)).normalize().toString();
    }

    public boolean isOneDeployResource() {
        return StringUtils.isNotBlank(getType());
    }

    @Override
    public String toString() {
        return "DeploymentResource{" + "type=" + type + ", " + super.toString() + '}';
    }
}
