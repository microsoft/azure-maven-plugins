/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    /**
     * Suppress NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE in findbugs, it just ignore the null checking and report error.
     * @param dir
     * @param fileList
     */
    public static void getAllFiles(final File dir, final List<File> fileList) {
        if (dir != null) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                fileList.add(file);
                if (file.isDirectory()) {
                    getAllFiles(file, fileList);
                }
            }
        }
    }

    public static void writeZipFile(final File directoryToZip, final List<File> fileList, final String zipFile)
        throws IOException {
        final FileOutputStream fos = new FileOutputStream(zipFile);
        final ZipOutputStream zos = new ZipOutputStream(fos);

        try {
            for (final File file : fileList) {
                if (!file.isDirectory()) {
                    addToZip(directoryToZip, file, zos);
                }
            }
        } finally {
            zos.close();
            fos.close();
        }
    }

    /**
     * Suppress OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE in findbugs here
     * We don't add try-catch-finally block for zos.closeEntry since is it too verbose
     * Just throws if any exception when closeEntry as wee declare it the method signature
     * @param directoryToZip
     * @param file
     * @param zos
     * @throws IOException
     */
    public static void addToZip(final File directoryToZip, final File file, final ZipOutputStream zos)
        throws IOException {
        final String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
            file.getCanonicalPath().length());
        final byte[] bytes = new byte[1024];
        final FileInputStream fos = new FileInputStream(file);
        int length;

        try {
            zos.putNextEntry(new ZipEntry(zipFilePath));
            while ((length = fos.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        } finally {
            zos.closeEntry();
            fos.close();
        }
    }
}
