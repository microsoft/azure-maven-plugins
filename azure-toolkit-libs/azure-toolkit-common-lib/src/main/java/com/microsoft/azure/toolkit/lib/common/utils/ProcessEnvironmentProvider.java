/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import java.util.Map;

public interface ProcessEnvironmentProvider {
    public Map<String, String> getEnvironment();
}
