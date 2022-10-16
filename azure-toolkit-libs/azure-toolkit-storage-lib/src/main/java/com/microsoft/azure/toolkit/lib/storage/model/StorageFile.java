/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import lombok.SneakyThrows;

import java.io.OutputStream;
import java.util.List;

public interface StorageFile {
    String getName();

    String getPath();

    @SneakyThrows
    String getId();

    boolean isDirectory();

    long getSize();

    void download(OutputStream output);

    List<? extends StorageFile> listFiles();
}
