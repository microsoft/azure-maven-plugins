/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.toolkit.lib.common.IProject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class JavaProject implements IProject {
    private String projectName;

    private Path baseDirectory;

    private Path classesOutputDirectory;

    private Path buildDirectory;

    private Path artifactFile;

    private List<Path> dependencies;

    @Override
    public Collection<Path> getProjectDependencies() {
        return dependencies;
    }

    @Override
    public boolean isWarProject() {
        return StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(artifactFile.getFileName().toString()), "war");
    }

    @Override
    public boolean isJarProject() {
        return StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(artifactFile.getFileName().toString()), "jar");
    }
}
