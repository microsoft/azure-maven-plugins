/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import org.apache.maven.model.Resource;

import java.util.List;

public interface ArtifactHandler {
    void publish(final List<Resource> resources) throws Exception;
}
