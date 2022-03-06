/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
@Builder
@EqualsAndHashCode
public class SpringCloudSku {
    @Nonnull
    private final String name;
    @Nonnull
    private final String tier;
    @Nonnull
    private final Integer capacity;
}
