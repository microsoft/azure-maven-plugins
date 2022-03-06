/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

public interface IArtifact {
    @Nullable
    File getFile();

    @Nonnull
    static FileArtifact fromFile(@Nonnull final File file) {
        return new FileArtifact(file);
    }

    @Getter
    class FileArtifact implements IArtifact {
        @Nonnull
        private final File file;

        public FileArtifact(@Nonnull final File file) {
            this.file = file;
        }
    }
}

