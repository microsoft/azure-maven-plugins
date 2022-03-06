/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;

@Getter
@Builder
@EqualsAndHashCode
public class SpringCloudPersistentDisk {
    @Nonnull
    private final Integer sizeInGB;
    @Nonnull
    private final Integer usedInGB;
    @Nonnull
    private final String mountPath;

    public String toString() {
        if (sizeInGB > 0) {
            String value = String.format("Path: %s, Size: %d GB", this.mountPath, this.sizeInGB);
            if (Objects.nonNull(usedInGB)) {
                value += String.format(", Used: %d GB", this.usedInGB);
            }
            return value;
        } else {
            return "---";
        }
    }
}
