/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class MavenArtifactUtils {
    private static final String[] ARTIFACT_EXTENSIONS = {"jar"};
    private static final String ARTIFACT_NOT_SUPPORTED = "Target file does not exist or is not executable, please " +
            "check the configuration.";
    private static final String MULTI_ARTIFACT = "Multiple artifacts(%s) could be deployed, please specify " +
            "the target artifact in plugin configurations.";

    @Nonnull
    public static Collection<File> getArtifactFiles(MavenProject project) {
        final String targetFolder = project.getBuild().getDirectory();
        final File directory = new File(targetFolder);
        if (!directory.isDirectory()) {
            return new ArrayList<>();
        }
        return FileUtils.listFiles(directory, ARTIFACT_EXTENSIONS, true);
    }

    public static boolean isExecutableJar(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file);
             final JarInputStream jarInputStream = new JarInputStream(fileInputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            return Optional.ofNullable(manifest).map(Manifest::getMainAttributes).map(a -> a.getValue("Main-Class")).isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    public static List<File> getArtifacts(@Nullable List<Resource> resources) {
        if (resources != null) {
            return resources.stream().flatMap(r -> getArtifacts(r).stream()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static List<File> getArtifacts(Resource resource) {
        if (CollectionUtils.isEmpty(resource.getIncludes())) {
            return Collections.emptyList();
        }
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(resource.getDirectory());
        directoryScanner.setIncludes(resource.getIncludes().toArray(new String[0]));
        final String[] exclude = resource.getExcludes() == null ? new String[0] :
                resource.getExcludes().toArray(new String[0]);
        directoryScanner.setExcludes(exclude);
        directoryScanner.scan();
        return Arrays.stream(directoryScanner.getIncludedFiles())
                .map(path -> new File(resource.getDirectory(), path))
                .collect(Collectors.toList());
    }

    @Nullable
    public static File getExecutableJarFiles(Collection<File> files) throws MojoExecutionException {
        if (files.isEmpty()) {
            return null;
        }
        final List<File> executableJars = files.stream().filter(MavenArtifactUtils::isExecutableJar).collect(Collectors.toList());
        if (executableJars.isEmpty()) {
            return null;
        }
        // Throw exception when there are multi runnable artifacts
        if (executableJars.size() > 1) {
            final String artifactNameLists = executableJars.stream()
                    .map(File::getName).collect(Collectors.joining(","));
            throw new MojoExecutionException(String.format(MULTI_ARTIFACT, artifactNameLists));
        }
        return executableJars.get(0);
    }
}
