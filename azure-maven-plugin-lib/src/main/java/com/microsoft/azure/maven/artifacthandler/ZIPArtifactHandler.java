/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

public class ZIPArtifactHandler implements IArtifactHandler<BaseDeployTarget> {
    // todo qisun
//    protected AbstractAzureMojo mojo;
//    public ZIPArtifactHandler(final AbstractAzureMojo mojo) {
//        this.mojo = mojo;
//    }

    @Override
    public void publish(BaseDeployTarget target) throws IOException, MojoExecutionException {
        target.zipDeploy(getZipFile());
    }

    protected File getZipFile() {
        return null;
    }
}
