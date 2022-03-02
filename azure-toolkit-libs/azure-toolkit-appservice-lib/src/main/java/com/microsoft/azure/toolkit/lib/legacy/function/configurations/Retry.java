/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.configurations;

import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.impl.DefaultFunctionProject;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.lang.annotation.Annotation;

@Getter()
@SuperBuilder(toBuilder = true)
public class Retry {
    private String strategy;
    private int maxRetryCount;
    private String delayInterval;
    private String minimumInterval;
    private String maximumInterval;

    public static Retry createFixedDelayRetryFromAnnotation(final FunctionAnnotation annotation) {
        return Retry.builder()
                .strategy(annotation.getStringValue("strategy", true))
                .maxRetryCount(Integer.valueOf(annotation.getStringValue("strategy", true)))
                .delayInterval(annotation.getStringValue("delayInterval", true)).build();
    }

    public static Retry createFixedDelayRetryFromAnnotation(final Annotation fixedDelayRetry) {
        return createFixedDelayRetryFromAnnotation(DefaultFunctionProject.create(fixedDelayRetry));
    }

    public static Retry createExponentialBackoffRetryFromAnnotation(final FunctionAnnotation annotation) {
        return Retry.builder()
                .strategy(annotation.getStringValue("strategy", true))
                .maxRetryCount(Integer.valueOf(annotation.getStringValue("strategy", true)))
                .minimumInterval(annotation.getStringValue("minimumInterval", true))
                .maximumInterval(annotation.getStringValue("maximumInterval", true)).build();
    }

    public static Retry createExponentialBackoffRetryFromAnnotation(final Annotation exponentialBackoffRetry) {
        return createExponentialBackoffRetryFromAnnotation(DefaultFunctionProject.create(exponentialBackoffRetry));
    }
}
