/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql.model;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class MySqlEntity implements IAzureResourceEntity {
    private String id;
    private String subscriptionId;
    private String resourceGroup;
    private String name;
    private Region region;

    /**
     * TODO(qianjin): for type, state and kind, give the possible values in javadoc
     */
    private String administratorLoginName;
    private String version;
    private String state;
    private String fullyQualifiedDomainName;
    private String type;

    private String skuTier;
    private int vCore;
    private int storageInMB;
    private String sslEnforceStatus;
}
