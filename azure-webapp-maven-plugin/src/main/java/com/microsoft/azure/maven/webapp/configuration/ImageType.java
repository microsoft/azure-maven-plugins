/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

public enum ImageType {
    NONE,
    BUILT_IN,
    PUBLIC_DOCKER_HUB,
    PRIVATE_DOCKER_HUB,
    PRIVATE_REGISTRY,
    UNKNOWN
}
