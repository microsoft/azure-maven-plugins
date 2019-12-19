/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.microsoft.azure.common.function.PackageHandler;

/**
 * Generate configuration files (host.json, function.json etc.) and copy JARs to staging directory.
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class PackageMojo extends AbstractFunctionMojo {

    @Override
    protected void doExecute() throws Exception {
    	new PackageHandler(context.getProject(), context.getDeploymentStagingDirectoryPath()).execute();
    }

}
