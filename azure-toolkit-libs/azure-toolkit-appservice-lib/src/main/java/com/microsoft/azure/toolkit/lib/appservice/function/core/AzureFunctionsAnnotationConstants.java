/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.core;

public class AzureFunctionsAnnotationConstants {
    // com.microsoft.azure.functions.annotation
    public static final String FUNCTION_NAME = "com.microsoft.azure.functions.annotation.FunctionName";
    public static final String STORAGE_ACCOUNT = "com.microsoft.azure.functions.annotation.StorageAccount";
    public static final String CUSTOM_BINDING = "com.microsoft.azure.functions.annotation.CustomBinding";
    public static final String FIXED_DELAY_RETRY = "com.microsoft.azure.functions.annotation.FixedDelayRetry";
    public static final String EXPONENTIAL_BACKOFF_RETRY = "com.microsoft.azure.functions.annotation.ExponentialBackoffRetry";

    // AuthorizationLevel
    public static final String ANONYMOUS = "ANONYMOUS";
    public static final String FUNCTION = "FUNCTION";
    public static final String ADMIN = "ADMIN";
}
