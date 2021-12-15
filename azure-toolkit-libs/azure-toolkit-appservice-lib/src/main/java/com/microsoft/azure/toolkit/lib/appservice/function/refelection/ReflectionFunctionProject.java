/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.refelection;

import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;

import java.util.List;

public class ReflectionFunctionProject extends FunctionProject {
    @Override
    public List<FunctionMethod> findAnnotatedMethods() {
        return MavenGradleFunctionStagingContributor.findAnnotatedMethods(this);
    }

    @Override
    public void installExtension(String funcPath) {
        MavenGradleFunctionStagingContributor.installExtension(this);
    }
}
