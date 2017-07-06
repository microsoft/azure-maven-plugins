/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;

/**
 * Interface for web-app deployment handler
 */
public interface DeployHandler {
    void validate(final WebApp app) throws Exception;

    void deploy(final WebApp app) throws Exception;
}
