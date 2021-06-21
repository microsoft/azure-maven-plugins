/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;

import java.io.File;

public interface IFunctionAppBase<T extends AppServiceBaseEntity> extends IAppService<T> {
    void deploy(File targetFile);

    void deploy(File targetFile, FunctionDeployType functionDeployType);

    String getMasterKey();
}
