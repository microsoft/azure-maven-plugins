/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.common.logging.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

// Todo: Merge this class with FTPUploader in lib to reduce duplicate
public class FTPUtils {

    public static final String REPLY_MESSAGE = "Reply Message : %s";
    public static final String FAILED_TO_UPLOAD_RESOURCE = "Failed to upload file: ";
    public static final String UPLOADING_RESOURCE = "Uploading resource %s to %s";

    public static FTPClient getFTPClient(final String ftpServer, final String username, final String password)
            throws IOException {
        final FTPClient ftpClient = new FTPClient();
        ftpClient.connect(ftpServer);
        ftpClient.login(username, password);
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }

    public static void uploadFile(final FTPClient ftpClient, final String sourceFilePath,
                                  final String targetFilePath) throws IOException {
        Log.info(String.format(UPLOADING_RESOURCE, sourceFilePath, targetFilePath));
        final File sourceFile = new File(sourceFilePath);
        changeDirectoryWithCreate(ftpClient, targetFilePath);
        try (final InputStream is = new FileInputStream(sourceFile)) {
            ftpClient.storeFile(sourceFile.getName(), is);
            final int replyCode = ftpClient.getReplyCode();
            final String replyMessage = ftpClient.getReplyString();
            if (isCommandFailed(replyCode)) {
                throw new IOException(FAILED_TO_UPLOAD_RESOURCE + sourceFilePath);
            } else {
                Log.info(String.format(REPLY_MESSAGE, replyMessage));
            }
        }
    }

    private static void changeDirectoryWithCreate(final FTPClient ftpClient, String targetPath) throws IOException {
        final Stack<Path> pathStacks = new Stack<>();
        Path path = Paths.get(targetPath);
        while (!ftpClient.changeWorkingDirectory(path.toString())) {
            pathStacks.push(path);
            path = path.getParent();
        }
        while (!pathStacks.isEmpty()) {
            ftpClient.makeDirectory(pathStacks.pop().toString());
        }
        ftpClient.changeWorkingDirectory(targetPath);
    }

    private static boolean isCommandFailed(final int replyCode) {
        return replyCode >= 300;
    }
}
