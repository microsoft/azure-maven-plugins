/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import java.util.Set;

public interface FunctionCoreToolsHandler {
    void installExtension(Set<Class> bindingTypes) throws Exception;
}
