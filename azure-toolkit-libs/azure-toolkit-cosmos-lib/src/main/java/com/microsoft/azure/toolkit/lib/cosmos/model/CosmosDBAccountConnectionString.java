package com.microsoft.azure.toolkit.lib.cosmos.model;

import org.jetbrains.annotations.Nullable;

public interface CosmosDBAccountConnectionString {
    @Nullable String getHost();

    @Nullable Integer getPort();

    @Nullable String getUsername();

    @Nullable String getPassword();

    @Nullable String getConnectionString();
}
