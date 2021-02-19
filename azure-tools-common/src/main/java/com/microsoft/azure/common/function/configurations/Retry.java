/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.configurations;

import com.microsoft.azure.functions.annotation.ExponentialBackoffRetry;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter()
@SuperBuilder(toBuilder = true)
public class Retry {
    private String strategy;
    private int maxRetryCount;
    private String delayInterval;
    private String minimumInterval;
    private String maximumInterval;

    public static Retry createFixedDelayRetryFromAnnotation(final FixedDelayRetry fixedDelayRetry) {
        return Retry.builder()
                .strategy(fixedDelayRetry.strategy())
                .maxRetryCount(fixedDelayRetry.maxRetryCount())
                .delayInterval(fixedDelayRetry.delayInterval()).build();
    }

    public static Retry createExponentialBackoffRetryFromAnnotation(final ExponentialBackoffRetry exponentialBackoffRetry) {
        return Retry.builder()
                .strategy(exponentialBackoffRetry.strategy())
                .maxRetryCount(exponentialBackoffRetry.maxRetryCount())
                .minimumInterval(exponentialBackoffRetry.minimumInterval())
                .maximumInterval(exponentialBackoffRetry.maximumInterval()).build();
    }
}
