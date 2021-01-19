/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.deploymentslot;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;
import com.microsoft.azure.toolkits.appservice.service.WebAppDeploymentSlotCreatable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

@Getter
public class WebAppDeploymentSlotCreatableImpl implements WebAppDeploymentSlotCreatable, WebAppDeploymentSlotCreatable.WithName {
    public static final String CONFIGURATION_SOURCE_NEW = "new";
    public static final String CONFIGURATION_SOURCE_PARENT = "parent";
    private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";

    private WebApp webAppModel;
    private String name;
    private String configurationSource = CONFIGURATION_SOURCE_PARENT;
    private Optional<Map<String, String>> appSettings = null;

    WebAppDeploymentSlotCreatableImpl(WebApp webAppModel) {
        this.webAppModel = webAppModel;
    }

    @Override
    public WebAppDeploymentSlotCreatable withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public WebAppDeploymentSlotCreatable withAppSettings(Map<String, String> appSettings) {
        this.appSettings = Optional.of(appSettings);
        return this;
    }

    @Override
    public WebAppDeploymentSlotCreatable withConfigurationSource(String configurationSource) {
        this.configurationSource = configurationSource;
        return this;
    }

    @Override
    public WebAppDeploymentSlot create() {
        final DeploymentSlot.DefinitionStages.Blank blank = webAppModel.deploymentSlots().define(getName());
        final DeploymentSlot.DefinitionStages.WithCreate withCreate;
        switch (StringUtils.lowerCase(configurationSource)) {
            case CONFIGURATION_SOURCE_NEW:
                withCreate = blank.withBrandNewConfiguration();
                break;
            case CONFIGURATION_SOURCE_PARENT:
                withCreate = blank.withConfigurationFromParent();
                break;
            default:
                final DeploymentSlot deploymentSlot = webAppModel.deploymentSlots().getByName(configurationSource);
                if (deploymentSlot == null) {
                    throw new RuntimeException(CONFIGURATION_SOURCE_DOES_NOT_EXISTS);
                }
                withCreate = blank.withConfigurationFromDeploymentSlot(deploymentSlot);
                break;
        }
        if (appSettings != null) {
            withCreate.withAppSettings(appSettings.get());
        }
        return WebAppDeploymentSlot.createFromServiceModel(withCreate.create());
    }
}
