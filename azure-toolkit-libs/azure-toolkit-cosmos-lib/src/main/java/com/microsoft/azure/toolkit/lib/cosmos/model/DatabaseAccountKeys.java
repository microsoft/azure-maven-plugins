/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.model;

import com.azure.resourcemanager.cosmos.models.DatabaseAccountListKeysResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DatabaseAccountKeys {
    private String primaryMasterKey;
    private String secondaryMasterKey;
    private String primaryReadonlyMasterKey;
    private String secondaryReadonlyMasterKey;

    public static DatabaseAccountKeys fromDatabaseAccountListKeysResult(@Nonnull DatabaseAccountListKeysResult result) {
        return DatabaseAccountKeys.builder()
                .primaryMasterKey(result.primaryMasterKey())
                .secondaryMasterKey(result.secondaryMasterKey())
                .primaryReadonlyMasterKey(result.primaryReadonlyMasterKey())
                .secondaryReadonlyMasterKey(result.secondaryReadonlyMasterKey())
                .build();
    }
}
