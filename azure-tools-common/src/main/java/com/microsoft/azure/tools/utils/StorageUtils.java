/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.tools.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.storage.file.CloudFile;

import java.io.File;
import java.net.URI;

public class StorageUtils {
    public static void uploadFileToStorage(File file, String sasUrl) throws AzureExecutionException {
        try {
            final CloudFile cloudFile = new CloudFile(new URI(sasUrl));
            cloudFile.uploadFromFile(file.getPath());
        } catch (Exception e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }
}
