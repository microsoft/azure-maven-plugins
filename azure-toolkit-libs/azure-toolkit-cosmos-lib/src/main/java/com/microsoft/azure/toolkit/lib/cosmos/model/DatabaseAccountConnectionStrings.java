/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import com.azure.resourcemanager.cosmos.models.DatabaseAccountConnectionString;
import com.azure.resourcemanager.cosmos.models.DatabaseAccountListConnectionStringsResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DatabaseAccountConnectionStrings {
    private String primaryConnectionString;
    private String secondaryConnectionString;
    private String primaryReadonlyConnectionString;
    private String secondaryReadonlyConnectionString;

    public static DatabaseAccountConnectionStrings fromDatabaseAccountListConnectionStringsResult(@Nonnull DatabaseAccountListConnectionStringsResult result,
                                                                                                  @Nonnull DatabaseAccountKind kind) {
        return DatabaseAccountConnectionStrings.builder()
                .primaryConnectionString(getConnectionString(result, kind, true, false))
                .secondaryConnectionString(getConnectionString(result, kind, false, false))
                .primaryReadonlyConnectionString(getConnectionString(result, kind, true, true))
                .secondaryReadonlyConnectionString(getConnectionString(result, kind, false, true))
                .build();
    }

    private static String getConnectionString(@Nonnull DatabaseAccountListConnectionStringsResult result, @Nonnull DatabaseAccountKind kind, boolean isPrimary, boolean isReadOnly) {
        return result.connectionStrings().stream()
                .filter(string -> StringUtils.contains(string.description(), kind.getValue()))
                .filter(string -> StringUtils.contains(string.description(), isPrimary ? "Primary" : "Secondary"))
                .filter(string -> isReadOnly == StringUtils.containsIgnoreCase(string.description(), "Read-Only"))
                .findFirst().map(DatabaseAccountConnectionString::connectionString).orElse(null);
    }
}
