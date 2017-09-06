/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

public interface DeployFacade {
    DeployFacade setupRuntime() throws Exception;

    DeployFacade applySettings() throws Exception;

    DeployFacade commitChanges() throws Exception;

    DeployFacade deployArtifacts() throws Exception;
}
