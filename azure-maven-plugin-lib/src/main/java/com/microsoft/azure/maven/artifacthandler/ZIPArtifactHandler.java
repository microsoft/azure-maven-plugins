/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

//import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

import java.io.File;

public class ZIPArtifactHandler implements ArtifactHandler {
    // todo qisun
//    protected AbstractAzureMojo mojo;

//    public ZIPArtifactHandler(final AbstractAzureMojo mojo) {
//        this.mojo = mojo;
//    }

    @Override
    public void publish(DeployTarget target) {
        target.zipDeploy(getZipFile());
    }

    protected File getZipFile() {
        return null;
    }
}
