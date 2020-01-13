/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.project;

import java.nio.file.Path;
import java.util.Collection;

public interface IProject {
    Path getBaseDirectory();

    Path getArtifactFile();

    Path getBuildDirectory();

    Path getClassesOutputDirectory();

    Collection<Path> getProjectDepencencies();

    boolean isWarProject();

    boolean isJarProject();

}
