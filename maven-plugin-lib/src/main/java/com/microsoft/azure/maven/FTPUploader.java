/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPUploader {
    public static final String UPLOAD_SUCCESS = "Successfully upload files to FTP server: ";
    public static final String UPLOAD_FAILURE = "Failed to upload files to FTP server: ";

    private FTPClient ftpClient;

    private Log log;

    public FTPUploader(final Log log) {
        this.log = log;
        this.ftpClient = new FTPClient();
    }

    public void uploadDirectoryWithRetries(final String ftpServer, final String username, final String password,
                                           final String sourceDirectory, final String targetDirectory,
                                           final int maxRetryCount) throws MojoExecutionException {
        boolean isSuccess = false;
        int retryCount = 0;
        while (retryCount < maxRetryCount) {
            retryCount++;
            if (uploadDirectory(ftpServer, username, password, sourceDirectory, targetDirectory)) {
                isSuccess = true;
                break;
            }
        }

        if (isSuccess) {
            log.info(UPLOAD_SUCCESS + ftpServer);
        } else {
            throw new MojoExecutionException(UPLOAD_FAILURE + ftpServer);
        }
    }

    public boolean uploadDirectory(final String ftpServer, final String username, final String password,
                                   final String sourceDirectoryPath, final String targetDirectoryPath) {
        log.info("FTP server URL: " + ftpServer);
        log.info("FTP username: " + username);
        boolean isSuccess = false;
        try {
            ftpClient.connect(ftpServer);
            ftpClient.login(username, password);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.changeWorkingDirectory(targetDirectoryPath);

            log.info("Start uploading directory: " + sourceDirectoryPath + " --> " + targetDirectoryPath);
            uploadDirectory(sourceDirectoryPath, targetDirectoryPath, "");
            log.info("Finish uploading directory: " + sourceDirectoryPath + " --> " + targetDirectoryPath);

            isSuccess = true;
            ftpClient.disconnect();
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("Failed to upload directory: " + sourceDirectoryPath + " --> " + targetDirectoryPath);
        }
        return isSuccess;
    }

    private void uploadDirectory(final String sourceDirectoryPath, final String targetDirectoryPath,
                                 final String logPrefix) throws IOException {
        log.info(logPrefix + "[DIR] " + sourceDirectoryPath + " --> " + targetDirectoryPath);
        final File sourceDirectory = new File(sourceDirectoryPath);
        final File[] files = sourceDirectory.listFiles();
        if (files == null || files.length == 0) {
            log.info(logPrefix + "Empty directory at " + sourceDirectoryPath);
            return;
        }

        // Make sure target directory exists
        final boolean isTargetDirectoryExist = ftpClient.changeWorkingDirectory(targetDirectoryPath);
        if (!isTargetDirectoryExist) {
            ftpClient.makeDirectory(targetDirectoryPath);
        }

        final String nextLevelPrefix = logPrefix + "..";
        for (File file : files) {
            if (file.isFile()) {
                uploadFile(file.getAbsolutePath(), targetDirectoryPath, nextLevelPrefix);
            } else {
                uploadDirectory(sourceDirectoryPath + "/" + file.getName(),
                        targetDirectoryPath + "/" + file.getName(), nextLevelPrefix);
            }
        }
    }

    public void uploadFile(final String sourceFilePath, final String targetFilePath, final String logPrefix)
            throws IOException {
        log.info(logPrefix + "[FILE] " + sourceFilePath + " --> " + targetFilePath);
        final File sourceFile = new File(sourceFilePath);
        final InputStream is = new FileInputStream(sourceFile);
        try {
            ftpClient.changeWorkingDirectory(targetFilePath);
            ftpClient.storeFile(sourceFile.getName(), is);
            log.info(String.format("%s.......Reply { code : %d, message : %s }",
                    logPrefix,
                    ftpClient.getReplyCode(),
                    ftpClient.getReplyString().trim()));
        } catch (Exception e) {
            log.error(e);
        } finally {
            is.close();
        }
    }
}
