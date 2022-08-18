/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import lombok.Builder;
import lombok.Data;

import java.net.MalformedURLException;
import java.net.URL;

@Data
@Builder
public class SqlDatabaseAccountConnectionString implements CosmosDBAccountConnectionString {
    private String uri;
    private String key;
    private String connectionString;

    @Override
    public String getHost() {
        try {
            return new URL(uri).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public Integer getPort() {
        try {
            return new URL(uri).getPort();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }
}
