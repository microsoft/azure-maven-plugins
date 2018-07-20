/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;

public interface IArtifactHandler <T extends BaseDeployTarget> {
    void publish(T deployTarget) throws Exception;
}
