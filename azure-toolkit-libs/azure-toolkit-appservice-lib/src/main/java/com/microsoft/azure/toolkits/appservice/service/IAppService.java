/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.Runtime;

import java.io.File;

public interface IAppService {
    void start();

    void stop();

    void restart();

    void delete();

    void deploy(File file);

    void deploy(DeployType deployType, File file);

    boolean exists();

    Runtime getRuntime();
}
