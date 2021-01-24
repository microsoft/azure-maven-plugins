/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import rx.Single;

public interface ICredentialRetriever {
    Single<AzureCredentialWrapper> retrieve();
}
