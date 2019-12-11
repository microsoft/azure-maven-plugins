/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.common.exceptions.AzureExecutionException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class FTPUploaderTest {
    @Mock
    Log log;

    private FTPUploader ftpUploader = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ftpUploader = new FTPUploader(log);
    }

    @Test
    public void uploadDirectoryWithRetries() throws Exception {
        final FTPUploader uploaderSpy = spy(ftpUploader);

        // Failure
        AzureExecutionException exception = null;
        doReturn(false)
                .when(uploaderSpy)
                .uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        try {
            uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password",
                    "sourceDir", "targetDir", 1);
        } catch (AzureExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }

        // Success
        doReturn(true)
                .when(uploaderSpy)
                .uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password",
                "sourceDir", "targetDir", 1);
    }

    @Test
    public void uploadDirectory() throws Exception {
        final FTPUploader uploaderSpy = spy(ftpUploader);
        final FTPClient ftpClient = mock(FTPClient.class);
        doReturn(ftpClient).when(uploaderSpy).getFTPClient(anyString(), anyString(), anyString());
        doNothing().when(uploaderSpy).uploadDirectory(any(FTPClient.class), anyString(), anyString(), anyString());

        uploaderSpy.uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(ftpClient, times(1)).disconnect();
        verifyNoMoreInteractions(ftpClient);
        verify(uploaderSpy, times(1)).uploadDirectory(any(FTPClient.class), anyString(), anyString(), anyString());
        verify(uploaderSpy, times(1)).getFTPClient(anyString(), anyString(), anyString());
        verify(uploaderSpy, times(1)).uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
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
            assertNotNull(caughtException);
        }
    }
}
