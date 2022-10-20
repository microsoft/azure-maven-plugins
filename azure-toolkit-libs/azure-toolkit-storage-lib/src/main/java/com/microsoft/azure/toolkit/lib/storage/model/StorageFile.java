/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import java.io.OutputStream;

public interface StorageFile extends AzResource {

    AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> getSubFileModule();

    Object getClient();

    boolean isDirectory();

    default long getSize() {
        if (this.isDirectory()) {
            return -1;
        }
        throw new AzureToolkitRuntimeException("Not implemented.");
    }

    void download(OutputStream output);
}
