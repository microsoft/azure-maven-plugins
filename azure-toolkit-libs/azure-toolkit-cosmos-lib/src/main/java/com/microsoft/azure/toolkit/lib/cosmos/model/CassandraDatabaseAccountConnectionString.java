/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

@Data
public class CassandraDatabaseAccountConnectionString implements CosmosDBAccountConnectionString {
    private String contactPoint;
    private Integer port;
    private String username;
    private String password;
    private String connectionString;

    public static CassandraDatabaseAccountConnectionString fromConnectionString(@Nonnull final String connectionString) {
        final String[] parameters = connectionString.split(";");
        final CassandraDatabaseAccountConnectionString result = new CassandraDatabaseAccountConnectionString();
        result.setContactPoint(extractValueFromParameters(parameters, "HostName"));
        result.setPort(Optional.ofNullable(extractValueFromParameters(parameters, "Port")).map(Integer::valueOf).orElse(null));
        result.setUsername(extractValueFromParameters(parameters, "Username"));
        result.setPassword(extractValueFromParameters(parameters, "Password"));
        result.setConnectionString(connectionString);
        return result;
    }

    @Nullable
    private static String extractValueFromParameters(@Nonnull final String[] parameters, final String key) {
        final String parameter = Arrays.stream(parameters).filter(value -> StringUtils.containsIgnoreCase(value, key)).findFirst().orElse(null);
        return StringUtils.isEmpty(parameter) ? null : StringUtils.substringAfter(parameter, "=");
    }

    @Override
    public String getHost() {
        return this.contactPoint;
    }
}
