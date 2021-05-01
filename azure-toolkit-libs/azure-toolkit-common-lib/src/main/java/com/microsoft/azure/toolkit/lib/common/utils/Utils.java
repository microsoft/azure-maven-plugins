/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.CommandExecuteException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class Utils {
    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");

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
}
