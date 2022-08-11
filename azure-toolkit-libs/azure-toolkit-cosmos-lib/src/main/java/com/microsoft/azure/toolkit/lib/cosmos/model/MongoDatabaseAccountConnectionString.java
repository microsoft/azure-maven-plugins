/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import com.mongodb.ConnectionString;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

@Data
public class MongoDatabaseAccountConnectionString {
    private String host;
    private Integer port;
    private List<String> hosts;
    private String username;
    private char[] password;
    private String connection;
    private Boolean sslEnabled;

    public static MongoDatabaseAccountConnectionString fromConnectionString(@Nonnull final String connectionString) {
        final ConnectionString mongo = new ConnectionString(connectionString);
        final MongoDatabaseAccountConnectionString result = new MongoDatabaseAccountConnectionString();
        result.setHosts(mongo.getHosts());
        result.setHost(CollectionUtils.isEmpty(mongo.getHosts()) ? null : StringUtils.substringBefore(mongo.getHosts().get(0), ":"));
        result.setPort(CollectionUtils.isEmpty(mongo.getHosts()) ? null : Integer.valueOf(StringUtils.substringAfter(mongo.getHosts().get(0), ":")));
        result.setUsername(mongo.getUsername());
        result.setPassword(mongo.getPassword());
        result.setSslEnabled(mongo.getSslEnabled());
        result.setConnection(connectionString);
        return result;
    }
}
