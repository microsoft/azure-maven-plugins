/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;

@Data
@EqualsAndHashCode
@Builder
public class DeploymentModel {
    private String format;
    private String name;
    private String version;
    private String source;

    public static DeploymentModel fromModel(@Nonnull final com.azure.resourcemanager.cognitiveservices.models.DeploymentModel deploymentModel) {
        return DeploymentModel.builder()
            .name(deploymentModel.name())
            .format(deploymentModel.format())
            .source(deploymentModel.source())
            .version(deploymentModel.version())
            .build();
    }

    public static DeploymentModel fromAccountModel(@Nonnull final AccountModel deploymentModel) {
        return DeploymentModel.builder()
            .name(deploymentModel.getName())
            .format(deploymentModel.getFormat())
            .source(deploymentModel.getSource())
            .version(deploymentModel.getVersion())
            .build();
    }

    public com.azure.resourcemanager.cognitiveservices.models.DeploymentModel toModel() {
        return new com.azure.resourcemanager.cognitiveservices.models.DeploymentModel()
            .withName(name)
            .withFormat(format)
            .withSource(source)
            .withVersion(version);
    }
}
