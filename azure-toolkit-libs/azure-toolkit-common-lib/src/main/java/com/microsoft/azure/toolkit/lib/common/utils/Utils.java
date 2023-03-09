/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.azure.resourcemanager.resources.fluentcore.utils.ResourceNamer;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.exception.CommandExecuteException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class Utils {
    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");
    private static final String POM = "pom";
    private static final String JAR = "jar";
    private static final String WAR = "war";
    private static final String EAR = "ear";
    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");

    public static String generateRandomResourceName(@Nonnull final String prefix, final int maxLength) {
        final String name = String.format("%s-%s", prefix, Utils.getTimestamp());
        return name.length() <= maxLength ? name : new ResourceNamer(StringUtils.EMPTY).getRandomName(name.substring(0, maxLength - 10), maxLength);
    }

    public static String getTimestamp() {
        return DATE_FORMAT.format(new Date());
    }

    public static int getJavaMajorVersion(final String javaVersion) {
        try {
            final String runtimeJavaMajorVersion = StringUtils.startsWith(javaVersion, "1.") ?
                    StringUtils.substring(javaVersion, 2, 3) : StringUtils.split(javaVersion, ".")[0];
            return Integer.valueOf(runtimeJavaMajorVersion);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static int getArtifactCompileVersion(File artifact) {
        try (JarFile jarFile = new JarFile(artifact)) {
            final Manifest manifest = jarFile.getManifest();
            final String startClass = manifest.getMainAttributes().getValue("Start-Class");
            final String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            final String target = StringUtils.isNotBlank(startClass) ? getJarEntryName("BOOT-INF/classes/" + startClass) :
                    StringUtils.isNotBlank(mainClass) ? getJarEntryName(mainClass) : null;
            final JarEntry jarEntry = StringUtils.isNotBlank(target) ? jarFile.getJarEntry(target) : jarFile.stream()
                    .filter(entry -> StringUtils.endsWith(entry.getName(), ".class"))
                    .findFirst().orElse(null);
            if (Objects.isNull(jarEntry)) {
                AzureMessager.getMessager().warning("Failed to parse artifact compile version, no class file founded in target artifact");
                return -1;
            }
            // Read compile version from class file
            // Refers https://en.wikipedia.org/wiki/Java_class_file#General_layout
            final InputStream stream = jarFile.getInputStream(jarEntry);
            final byte[] version = new byte[2];
            stream.skip(6);
            stream.read(version);
            stream.close();
            return new BigInteger(version).intValueExact() - 44;
        } catch (IOException e) {
            AzureMessager.getMessager().warning(String.format("Failed to parse artifact compile version: %s", e.getMessage()));
            return -1;
        }
    }

    @Nonnull
    private static String getJarEntryName(@Nonnull final String className){
        final String fullName = StringUtils.replace(className, ".", "/");
        return fullName + ".class";
    }

    public static boolean isGUID(String input) {
        try {
            return UUID.fromString(input).toString().equalsIgnoreCase(input);
        } catch (Exception e) {
            return false;
        }
    }

    // Copied from https://github.com/microsoft/azure-tools-for-java/blob/azure-intellij-toolkit-v3.39.0/Utils/
    // azuretools-core/src/com/microsoft/azuretools/core/mvp/model/AzureMvpModel.java
    // Todo: Remove duplicated utils function in azure-tools-for-java
    public static String getSegment(String id, String segment) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        final String[] attributes = StringUtils.lowerCase(id).split("/");
        final int pos = ArrayUtils.indexOf(attributes, StringUtils.lowerCase(segment));
        if (pos >= 0) {
            return attributes[pos + 1];
        }
        return null;
    }

    public static String getSubscriptionId(String resourceId) {
        return getSegment(resourceId, SUBSCRIPTIONS);
    }

    public static boolean isPomPackagingProject(String packaging) {
        return POM.equalsIgnoreCase(packaging);
    }

    public static boolean isJarPackagingProject(String packaging) {
        return JAR.equalsIgnoreCase(packaging);
    }

    public static boolean isWarPackagingProject(String packaging) {
        return WAR.equalsIgnoreCase(packaging);
    }

    public static boolean isEarPackagingProject(String packaging) {
        return EAR.equalsIgnoreCase(packaging);
    }

    public static String calcXmlIndent(String[] lines, int row, int column) {
        Preconditions.checkNotNull(lines, "The parameter 'lines' cannot be null");
        Preconditions.checkArgument(lines.length > row && row >= 0, "The parameter 'row' overflows.");
        final String line = lines[row];
        Preconditions.checkArgument(line != null, "Encounter null on row: " + row);
        Preconditions.checkArgument(line.length() >= column && column >= 0, "The parameter 'column' overflows");

        final StringBuilder buffer = new StringBuilder();
        final int pos = line.lastIndexOf('<', column) - 1; // skip the current tag like : <tag>
        for (int i = 0; i <= pos; i++) {
            if (line.charAt(i) == '\t') {
                buffer.append('\t');
            } else {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }

    public static String executeCommandAndGetOutput(final String cmd, File cwd)
        throws IOException, InterruptedException {
        final String[] cmds = new String[]{isWindows ? "cmd.exe" : "bash", isWindows ? "/c" : "-c", cmd};
        final Process p = Runtime.getRuntime().exec(cmds, null, cwd);
        final int exitCode = p.waitFor();
        if (exitCode != 0) {
            String errorLog = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
            throw new CommandExecuteException(String.format("Cannot execute '%s' due to error: %s", cmd, errorLog));
        }
        return IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
    }

    public static Collection<String> intersectIgnoreCase(List<String> list1, List<String> list2) {
        if (CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
            return list2.stream().filter(str -> containsIgnoreCase(list1, str)).collect(Collectors.toSet());
        }
        return Collections.emptyList();
    }

    public static boolean containsIgnoreCase(List<String> list, String str) {
        if (StringUtils.isNotBlank(str) && CollectionUtils.isNotEmpty(list)) {
            return list.stream().anyMatch(str2 -> StringUtils.equalsIgnoreCase(str, str2));
        }
        return false;
    }

    public static String getId(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

    public static <K, V> Map<K, V> groupByIgnoreDuplicate(Collection<V> list, Function<? super V, ? extends K> keyMapper) {
        return list.stream().collect(Collectors.toMap(keyMapper, item -> item, (item1, item2) -> item1));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static <T> T selectFirstOptionIfCurrentInvalid(String name, List<T> options, T value) {
        if (options.isEmpty()) {
            throw new AzureToolkitRuntimeException(String.format("No %s is available.", name));
        }
        return options.contains(value) ? value : options.get(0);
    }

    public static <T> void copyProperties(T to, T from, boolean whenNotSet) throws IllegalAccessException {
        for (Field field : FieldUtils.getAllFields(from.getClass())) {
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            final Object fromValue = FieldUtils.readField(field, from, true);
            final Object toValue = FieldUtils.readField(field, to, true);
            final Class<?> type = field.getType();
            final boolean isCustomObject = !(type.getName().startsWith("java") || type.isPrimitive() || type.isEnum() || type.isAssignableFrom(String.class) || type.isArray());
            if (isCustomObject && ObjectUtils.allNotNull(fromValue, toValue)) {
                copyProperties(toValue, fromValue, whenNotSet);
            } else {
                if ((!whenNotSet || toValue == null) && fromValue != null) {
                    FieldUtils.writeField(field, to, fromValue, true);
                }
            }
        }
    }
}
