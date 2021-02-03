/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.models;

import com.microsoft.azure.toolkits.appservice.model.DeployType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.io.File;

@Getter
@SuperBuilder(toBuilder = true)
public class MavenArtifact {
    private File file;
    private String path;
    private DeployType deployType;
}
