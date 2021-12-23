/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Optional;

public class SpringCloudClusterModule extends AbstractAzResourceModule<SpringCloudCluster, SpringCloudResourceManager, SpringService> {

    public static final String NAME = "Spring";

    public SpringCloudClusterModule(@Nonnull SpringCloudResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public SpringServices getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppPlatformManager::springServices).orElse(null);
    }

    @Override
    protected SpringCloudCluster newDraft(@Nonnull String name, @Nonnull String resourceGroup) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nonnull
    protected SpringCloudCluster newResource(@Nonnull SpringService r) {
        return new SpringCloudCluster(r, this);
    }
}
