/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;

import org.apache.maven.model.Resource;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    private static final String CREATE_TEMP_FILE_FAIL = "Failed to create temp file %s.%s";

    public static File createTempFile(final String prefix, final String suffix) throws AzureExecutionException {
        try {
            final File zipFile = File.createTempFile(prefix, suffix);
            zipFile.deleteOnExit();
            return zipFile;
        } catch (IOException e) {
            throw new AzureExecutionException(String.format(CREATE_TEMP_FILE_FAIL, prefix, suffix), e.getCause());
        }
    }

    // Todo: Move this method to common for duplicated with Utils in spring maven plugin
    public static List<File> getArtifacts(Resource resource) {
        final List<File> result = new ArrayList<>();
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
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
        return result;
    }

    private Utils(){

    }
}
