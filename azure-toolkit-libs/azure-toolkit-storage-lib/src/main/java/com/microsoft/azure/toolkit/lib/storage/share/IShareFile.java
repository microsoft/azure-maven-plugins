/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;

public interface IShareFile extends StorageFile {
    Share getShare();
}
