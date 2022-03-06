/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database;

import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Data
public class DatabaseServerConfig {
    @Nonnull
    private String name;
    @Nullable
    private ResourceGroup resourceGroup;
    @Nullable
    private Subscription subscription;
    @Nonnull
    private Region region;

    private String adminName;
    private String adminPassword;
    private String version;
    private String fullyQualifiedDomainName;
    private boolean azureServiceAccessAllowed;
    private boolean localMachineAccessAllowed;

    @Nullable
    public String getSubscriptionId() {
        return Objects.nonNull(subscription) ? subscription.getId() : null;
    }

    @Nullable
    public String getResourceGroupName() {
        return Objects.nonNull(resourceGroup) ? resourceGroup.getName() : null;
    }
}
