/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.azure.storage.file.CloudFile;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    private static final String POM = "pom";
    private static final String JAR = "jar";
    private static final String MEMORY_REGEX = "(\\d+(\\.\\d+)?)([a-zA-Z]+)";
    private static final Pattern MEMORY_PATTERN = Pattern.compile(MEMORY_REGEX);
    private static final String[] ARTIFACT_EXTENSIONS = {"jar"};
    private static final int RESOURCE_INTERVAL = 1;
    private static final String ARTIFACT_NOT_SUPPORTED = "Target file does not exist or is not executable, please " +
            "check the configuration.";
    private static final String MULTI_ARTIFACT = "Multiple artifacts(%s) could be deployed, please specify " +
            "the target artifact in plugin configurations.";

    public static int convertSizeStringToNumber(String memory) throws MojoExecutionException {
        final Matcher matcher = MEMORY_PATTERN.matcher(memory);
        if (!matcher.matches()) {
            throw new MojoExecutionException("Format Exception");
        }
        final double number = Double.valueOf(matcher.group(1));
        final String unit = matcher.group(3).toLowerCase();
        switch (unit) {
            case "m":
            case "mb":
                return (int) (Math.ceil(number / 1024));
            case "g":
            case "gb":
                return (int) Math.ceil(number);
            case "t":
            case "tb":
                return (int) Math.ceil(1024 * number);
            default:
                throw new MojoExecutionException("Unknown memory unit");
        }
    }

    public static File getArtifactFromTargetFolder(MavenProject project) throws MojoExecutionException {
        final String targetFolder = project.getBuild().getDirectory();
        final Collection<File> files = FileUtils.listFiles(new File(targetFolder), ARTIFACT_EXTENSIONS, true);
        return getRunnableArtifactFromFiles(files);
    }

    public static File getArtifactFromConfiguration(SpringConfiguration springConfiguration) throws MojoExecutionException {
        final Deployment deployment = springConfiguration.getDeployment();
        final List<File> files = getArtifacts(deployment.getResources());
        return getRunnableArtifactFromFiles(files);
    }

    public static void uploadFileToStorage(File file, String sasUrl) throws MojoExecutionException {
        try {
            final CloudFile cloudFile = new CloudFile(new URI(sasUrl));
            cloudFile.uploadFromFile(file.getPath());
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public static boolean isPomPackagingProject(MavenProject mavenProject) {
        return POM.equalsIgnoreCase(mavenProject.getPackaging());
    }

    public static boolean isJarPackagingProject(MavenProject mavenProject) {
        return JAR.equalsIgnoreCase(mavenProject.getPackaging());
    }

    public static boolean isExecutableJar(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file);
             final JarInputStream jarInputStream = new JarInputStream(fileInputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            return manifest.getMainAttributes().getValue("Main-Class") != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get resource repeatedly until it match the predicate or timeout
     * @param callable callable to get resource
     * @param predicate function that evaluate the resource
     * @param timeOutInSeconds max time for the method
     * @return the first resource which fit the predicate or the last result before timeout
     */
    public static <T> T getResourceWithPredicate(Callable<T> callable, Predicate<T> predicate, int timeOutInSeconds) {
        final long timeout = System.currentTimeMillis() + timeOutInSeconds * 1000;
        return Observable.interval(RESOURCE_INTERVAL, TimeUnit.SECONDS)
                .flatMap(aLong -> Observable.fromCallable(callable))
                .subscribeOn(Schedulers.io())
                .takeUntil(resource -> predicate.test(resource) || System.currentTimeMillis() > timeout)
                .toBlocking().last();
    }

    private static List<File> getArtifacts(List<Resource> resources) {
        final List<File> result = new ArrayList<>();
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        for (final Resource resource : resources) {
            if (resource.getIncludes() != null && resource.getIncludes().size() > 0) {
                directoryScanner.setBasedir(resource.getDirectory());
                directoryScanner.setIncludes(resource.getIncludes().toArray(new String[0]));
                final String[] exclude = resource.getExcludes() == null ? new String[0] :
                        resource.getExcludes().toArray(new String[0]);
                directoryScanner.setExcludes(exclude);
                directoryScanner.scan();
                final List<File> resourceFiles = Arrays.stream(directoryScanner.getIncludedFiles())
                        .map(path -> new File(resource.getDirectory(), path))
                        .collect(Collectors.toList());
                result.addAll(resourceFiles);
            }
        }
        return result;
    }

    private static File getRunnableArtifactFromFiles(Collection<File> files) throws MojoExecutionException {
        final List<File> runnableArtifacts = files.stream().filter(Utils::isExecutableJar).collect(Collectors.toList());
        if (runnableArtifacts.isEmpty()) {
            throw new MojoExecutionException(ARTIFACT_NOT_SUPPORTED);
        }
        // Throw exception when there are multi runnable artifacts
        if (runnableArtifacts.size() > 1) {
            final String artifactNameLists = runnableArtifacts.stream()
                    .map(File::getName).collect(Collectors.joining(","));
            throw new MojoExecutionException(String.format(MULTI_ARTIFACT, artifactNameLists));
        }
        return runnableArtifacts.get(0);
    }

    private Utils() {
    }
}
