/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.common.project.IProject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ProjectUtils {
    public static IProject convertCommonProject(MavenProject project) {
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

    private static class JavaProject implements IProject {

        private String projectName;

        private Path baseDirectory;

        private Path classesOutputDirectory;

        private Path buildDirectory;

        private Path artifactFile;

        private List<Path> dependencies;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public Path getBaseDirectory() {
            return baseDirectory;
        }

        public void setBaseDirectory(Path baseDirectory) {
            this.baseDirectory = baseDirectory;
        }

        public Path getClassesOutputDirectory() {
            return classesOutputDirectory;
        }

        public void setClassesOutputDirectory(Path classesOutputDirectory) {
            this.classesOutputDirectory = classesOutputDirectory;
        }

        public Path getBuildDirectory() {
            return buildDirectory;
        }

        public void setBuildDirectory(Path buildDirectory) {
            this.buildDirectory = buildDirectory;
        }

        public Path getArtifactFile() {
            return artifactFile;
        }

        public void setArtifactFile(Path artifactFile) {
            this.artifactFile = artifactFile;
        }

        public List<Path> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<Path> dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public Collection<Path> getProjectDepencencies() {
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
}
