package com.microsoft.azure.toolkit.lib.cosmos.model;

import javax.annotation.Nullable;

public interface CosmosDBAccountConnectionString {
    @Nullable String getHost();

    @Nullable Integer getPort();

    @Nullable String getUsername();

    @Nullable String getPassword();

    @Nullable String getConnectionString();
}
