/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

//import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;

import java.io.File;

public class ZIPArtifactHandler implements IArtifactHandler {
    // todo qisun
//    protected AbstractAzureMojo mojo;

//    public ZIPArtifactHandler(final AbstractAzureMojo mojo) {
//        this.mojo = mojo;
//    }

    @Override
    public void publish(BaseDeployTarget target) {
        target.zipDeploy(getZipFile());
    }

    protected File getZipFile() {
        return null;
    }
}
