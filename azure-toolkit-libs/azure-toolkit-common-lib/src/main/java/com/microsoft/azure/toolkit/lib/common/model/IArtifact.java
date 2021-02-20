/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import lombok.Getter;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

public interface IArtifact {
    File getFile();

    @Nullable
    static FileArtifact fromFile(@Nullable File file) {
        return Optional.ofNullable(file).map(FileArtifact::new).orElse(null);
    }

    @Getter
    class FileArtifact implements IArtifact {

        private final File file;

        public FileArtifact(File file) {
            this.file = file;
        }
    }
}

