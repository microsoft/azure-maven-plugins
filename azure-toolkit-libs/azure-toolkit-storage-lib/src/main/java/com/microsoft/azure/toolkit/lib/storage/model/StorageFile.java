/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public interface StorageFile extends AzResource {

    AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> getSubFileModule();

    Object getClient();

    boolean isDirectory();

    String getPath();

    String getUrl();

    String getSasUrl();

    default long getSize() {
        if (this.isDirectory()) {
            return -1;
        }
        throw new AzureToolkitRuntimeException("Not implemented.");
    }

    void download(OutputStream output);

    void download(Path dest);

    default StorageFile getFile(String relativePath) {
        if (StringUtils.isEmpty(relativePath) || StringUtils.equals(relativePath.trim(), ".")) {
            return this;
        }
        final Path path = Paths.get(relativePath);
        StorageFile current = this;
        for (int i = 0; i < path.getNameCount(); i++) {
            final String name = path.getName(i).toString();
            current = current.getSubFileModule().get(name, null);
            if (Objects.isNull(current)) {
                return null;
            }
        }
        return current;
    }

    interface Draft<T extends StorageFile, R> extends AzResource.Draft<T, R> {
        void setDirectory(Boolean directory);

        void setSourceFile(Path source);
    }
}
