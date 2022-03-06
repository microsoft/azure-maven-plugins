/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;

@Getter
@Builder
@EqualsAndHashCode
@Deprecated
public class ScaleSettings {
    @Nonnull
    private final Integer cpu;
    @Nonnull
    private final Integer memoryInGB;
    @Nonnull
    private final Integer capacity;

    public boolean isEmpty() {
        return ObjectUtils.allNull(this.cpu, this.memoryInGB, this.capacity);
    }
}
