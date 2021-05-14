/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;

public interface IWebAppBase<T extends IAzureResourceEntity> extends IAppService<T>, IOneDeploy{
}
