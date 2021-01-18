/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Utils {
    public static Collection<String> intersectIgnoreCase(List<String> list1, List<String> list2) {
        final List<String> intersection = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list1) && CollectionUtils.isNotEmpty(list2)) {
            for (final String str : list2) {
                if (containsIgnoreCase(list1, str)) {
                    intersection.add(str);
                }
            }
            return intersection;
        }
        return Collections.emptyList();
    }

    private static boolean containsIgnoreCase(List<String> list, String str) {
        if (StringUtils.isNotBlank(str) && CollectionUtils.isNotEmpty(list)) {
            return list.stream().anyMatch(str2 -> StringUtils.equalsIgnoreCase(str, str2));
        }
        return false;
    }
}
