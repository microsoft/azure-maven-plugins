/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.webapp.deployadapter.IDeployAdapter;

public interface ArtifactHandler {
    void publish(IDeployAdapter deployTarget) throws Exception;
}
