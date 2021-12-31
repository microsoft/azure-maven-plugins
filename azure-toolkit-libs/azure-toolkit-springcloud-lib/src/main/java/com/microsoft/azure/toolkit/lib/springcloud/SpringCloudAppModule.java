/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringApps;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Optional;

public class SpringCloudAppModule extends AbstractAzResourceModule<SpringCloudApp, SpringCloudCluster, SpringApp> {

    public static final String NAME = "apps";

    public SpringCloudAppModule(@Nonnull SpringCloudCluster parent) {
        super(NAME, parent);
    }

    @Override
    public SpringApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SpringService::apps).orElse(null);
    }

    @Override
    protected SpringCloudAppDraft newDraft(@Nonnull String name, @Nonnull String resourceGroup) {
        return new SpringCloudAppDraft(name, this);
    }

    @Nonnull
    protected SpringCloudApp newResource(@Nonnull SpringApp remote) {
        return new SpringCloudApp(remote, this);
    }
}
