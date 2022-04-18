/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.fluent.models.HostKeysInner;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlotBasic;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class FunctionAppDeploymentSlot extends FunctionAppBase<FunctionAppDeploymentSlot, FunctionApp, FunctionDeploymentSlot> {

    protected FunctionAppDeploymentSlot(@Nonnull String name, @Nonnull FunctionAppDeploymentSlotModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    protected FunctionAppDeploymentSlot(@Nonnull FunctionAppDeploymentSlot origin) {
        super(origin);
    }

    protected FunctionAppDeploymentSlot(@Nonnull FunctionDeploymentSlotBasic remote, @Nonnull FunctionAppDeploymentSlotModule module) {
        super(remote.name(), module);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, FunctionAppDeploymentSlot, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Override
    public String getMasterKey() {
        final String name = String.format("%s/slots/%s", getParent().getName(), this.getName());
        return getFullRemote().manager().serviceClient().getWebApps().listHostKeysAsync(this.getResourceGroupName(), name).map(HostKeysInner::masterKey).block();
    }
}
