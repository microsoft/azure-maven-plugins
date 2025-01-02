/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.containerapps.config;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppContainerMavenConfig {

    @Nullable
    private Double cpu;
    @Nullable
    private String memory;
    private String type;
    @Nullable
    private String image;
    @Nullable
    private List<EnvironmentVar> environment;
    @Nullable
    private String directory;

    public DeploymentType getDeploymentType() {
        return StringUtils.isBlank(type) ? DeploymentType.IMAGE : DeploymentType.valueOf(type.toUpperCase());
    }
}
