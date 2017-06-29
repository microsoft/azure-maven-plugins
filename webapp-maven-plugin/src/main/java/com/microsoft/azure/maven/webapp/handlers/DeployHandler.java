/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.OperationResult;

/**
 * Interface for web-app deployment handler
 */
public interface DeployHandler {
    OperationResult validate(final WebApp app);

    OperationResult deploy(final WebApp app);
}
