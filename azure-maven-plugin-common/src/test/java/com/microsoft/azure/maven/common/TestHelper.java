/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common;

import com.google.common.base.Preconditions;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestHelper {
    public static String joinIntegers(List<Integer> integers) {
        Preconditions.checkNotNull(integers, "Parameter 'integers' should not be null or empty.");
        return integers.stream().map(t -> t.toString()).collect(Collectors.joining(","));
    }

    public static Model readMavenModel(File pomFile) throws IOException {
        final ModelReader reader = new DefaultModelReader();
        return reader.read(pomFile, Collections.emptyMap());
    }
}
