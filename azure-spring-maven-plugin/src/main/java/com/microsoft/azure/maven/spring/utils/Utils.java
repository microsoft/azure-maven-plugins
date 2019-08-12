/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import com.microsoft.azure.maven.spring.SpringConfiguration;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.storage.file.CloudFile;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final String DATE_FORMAT = "yyyyMMddHHmmss";
    private static final String MEMORY_REGEX = "(\\d+(\\.\\d+)?)([a-zA-Z]+)";
    private static final Pattern MEMORY_PATTERN = Pattern.compile(MEMORY_REGEX);
    private static final String POM = "pom";
    private static final String JAR = "jar";

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

    public static List<File> getArtifacts(List<Resource> resources) {
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

    public static String generateTimestamp() {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date());
    }

    public static File getArtifactFromConfiguration(SpringConfiguration springConfiguration) {
        final Deployment deployment = springConfiguration.getDeployment();
        final List<File> files = getArtifacts(deployment.getResources());
        return files.parallelStream().filter(file -> isExecutableJar(file)).findFirst().orElse(null);
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

    public static void replaceDomWithKeyValue(Xpp3Dom node, String key, Object value) {
        if (value == null) {
            return;
        }
        if (node.getChild(key) == null) {
            node.addChild(createDomWithKeyValue(key, value.toString()));
        } else {
            node.getChild(key).setValue(value.toString());
        }
    }

    public static Xpp3Dom createDomWithName(String name) {
        return new Xpp3Dom(name);
    }

    public static Xpp3Dom createDomWithKeyValue(String name, String value) {
        final Xpp3Dom dom = new Xpp3Dom(name);
        dom.setValue(value);
        return dom;
    }

    private static boolean isExecutableJar(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file);
                final JarInputStream jarInputStream = new JarInputStream(fileInputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            return manifest.getMainAttributes().getValue("Main-Class") != null;
        } catch (IOException e) {
            return false;
        }
    }

    private Utils() {
    }
}
