/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.resourcemanager.cognitiveservices.fluent.models.DeploymentInner;
import com.azure.resourcemanager.cognitiveservices.models.Deployment;
import com.azure.resourcemanager.cognitiveservices.models.DeploymentProperties;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentSku;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CognitiveDeployment extends AbstractAzResource<CognitiveDeployment, CognitiveAccount, Deployment>
    implements Deletable {

    protected CognitiveDeployment(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CognitiveDeploymentModule module) {
        super(name, resourceGroupName, module);
    }

    protected CognitiveDeployment(@Nonnull CognitiveDeployment deployment) {
        super(deployment);
    }

    protected CognitiveDeployment(@Nonnull Deployment remote, @Nonnull CognitiveDeploymentModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull Deployment remote) {
        return remote.innerModel().properties().provisioningState().toString();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    public DeploymentModel getModel(){
        return Optional.ofNullable(this.getRemote()).map(Deployment::innerModel)
            .map(DeploymentInner::properties)
            .map(DeploymentProperties::model)
            .map(DeploymentModel::fromModel).orElse(null);
    }

    public DeploymentSku getSku() {
        return Optional.ofNullable(this.getRemote()).map(Deployment::innerModel)
            .map(DeploymentInner::sku)
            .map(DeploymentSku::fromSku).orElse(null);
    }
}
