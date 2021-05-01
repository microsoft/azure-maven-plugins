/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.FunctionConfiguration;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AnnotationHandler {
    Set<Method> findFunctions(final List<URL> urls);

    Map<String, FunctionConfiguration> generateConfigurations(final Set<Method> methods) throws AzureExecutionException;

    FunctionConfiguration generateConfiguration(final Method method) throws AzureExecutionException;
}
