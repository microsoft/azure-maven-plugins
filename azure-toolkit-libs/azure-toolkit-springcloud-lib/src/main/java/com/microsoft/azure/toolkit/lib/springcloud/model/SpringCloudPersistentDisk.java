/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
@EqualsAndHashCode
public class SpringCloudPersistentDisk {
    private final Integer sizeInGB;
    private final Integer usedInGB;
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
