/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.account;

import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import java.util.List;

public interface IAzureAccount extends AzureService {
    IAccount account();

    List<Region> listRegions(String subscriptionId);
}
