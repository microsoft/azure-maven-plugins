/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class FTPUploaderTest {
    private FTPUploader ftpUploader = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ftpUploader = new FTPUploader();
    }

    @Test
    public void uploadDirectoryWithRetries() throws Exception {
        final FTPUploader uploaderSpy = Mockito.spy(ftpUploader);

        // Failure
        AzureExecutionException exception = null;
        Mockito.doReturn(false).when(uploaderSpy).uploadDirectory(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString());
        try {
            uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password", "sourceDir", "targetDir", 1);
        } catch (AzureExecutionException e) {
            exception = e;
        } finally {
            Assert.assertNotNull(exception);
        }

        // Success
        Mockito.doReturn(true).when(uploaderSpy).uploadDirectory(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString());
        uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password", "sourceDir", "targetDir", 1);
    }

    @Test
    public void uploadDirectory() throws Exception {
        final FTPUploader uploaderSpy = Mockito.spy(ftpUploader);
        final FTPClient ftpClient = Mockito.mock(FTPClient.class);
        Mockito.doReturn(ftpClient).when(uploaderSpy).getFTPClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.doNothing().when(uploaderSpy).uploadDirectory(ArgumentMatchers.any(FTPClient.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

        uploaderSpy.uploadDirectory(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verify(ftpClient, Mockito.times(1)).disconnect();
        Mockito.verifyNoMoreInteractions(ftpClient);
        Mockito.verify(uploaderSpy, Mockito.times(1)).uploadDirectory(ArgumentMatchers.any(FTPClient.class), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verify(uploaderSpy, Mockito.times(1)).getFTPClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.verify(uploaderSpy, Mockito.times(1)).uploadDirectory(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        verifyNoMoreInteractions(uploaderSpy);
    }

    @Test
    public void getFTPClient() throws Exception {
        Exception caughtException = null;
        try {
            ftpUploader.getFTPClient("fakeFTPServer", "username", "password");
        } catch (Exception e) {
            caughtException = e;
        } finally {
            Assert.assertNotNull(caughtException);
        }
    }
}
