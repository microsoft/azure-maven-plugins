/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;

public class AzureOperationTitleBundle extends AzureMessageBundle implements AzureOperationBundle.Provider {

    public AzureOperationTitleBundle(String toolPostfix) {
        super("com.microsoft.azure.toolkit.operation.titles", toolPostfix);
    }
}
