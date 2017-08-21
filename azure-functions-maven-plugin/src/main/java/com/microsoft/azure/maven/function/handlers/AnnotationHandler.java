/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public interface AnnotationHandler {
    Set<Method> findFunctions(final URL url);

    Map<String, FunctionConfiguration> generateConfigurations(final Set<Method> methods) throws Exception;

    FunctionConfiguration generateConfiguration(final Method method) throws Exception;
}
