/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core;

import com.microsoft.azure.toolkit.lib.auth.model.AccountProfile;

public interface IProfileBuilder {
    AccountProfile buildProfile();
}
