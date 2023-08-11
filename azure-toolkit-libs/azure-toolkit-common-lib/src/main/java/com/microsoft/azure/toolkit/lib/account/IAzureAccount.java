/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.account;

import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import java.util.List;

public interface IAzureAccount extends AzService {
    IAccount account();

    boolean isLoggedIn();

    boolean isLoggingIn();

    List<Region> listRegions(String subscriptionId);
}
