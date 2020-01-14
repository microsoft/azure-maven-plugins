/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.common.project.JavaProject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProjectUtils {
    public static IProject convertCommonProject(MavenProject project) {
        if (project == null) {
            return null;
        }
        final JavaProject proj = new JavaProject();
        proj.setProjectName(project.getName());
        proj.setBaseDirectory(project.getBasedir().toPath());
        final String artifactFileName = project.getBuild().getFinalName() + "." + project.getPackaging();
        proj.setArtifactFile(Paths.get(project.getBuild().getDirectory(), artifactFileName));
        proj.setClassesOutputDirectory(Paths.get(project.getBuild().getOutputDirectory()));
        proj.setDependencies(collectDependencyPaths(project.getArtifacts()));
        proj.setBuildDirectory(Paths.get(project.getBuild().getDirectory()));
        return proj;
    }

    private static List<Path> collectDependencyPaths(Set<Artifact> dependencies) {
        final List<Path> results = new ArrayList<>();
        for (final Artifact artifact : dependencies) {
            results.add(artifact.getFile().toPath());
        }
        return results;
    }
}
