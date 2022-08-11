/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SqlDatabaseAccountConnectionString {
    private String uri;
    private String key;
    private String connectionString;
}
