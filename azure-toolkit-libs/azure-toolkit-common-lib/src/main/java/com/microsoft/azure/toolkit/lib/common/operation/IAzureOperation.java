/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

public interface IAzureOperation {
    String getId();

    String getName();

    String getType();

    Object getTitle();
}
