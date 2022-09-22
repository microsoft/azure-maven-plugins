/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;

public interface IDeploymentSlotModule<T extends AbstractAzResource<T, P, R>, P extends AbstractAzResource<P, ?, ?>, R> extends
        AzResourceModule<T, P, R> {
}
