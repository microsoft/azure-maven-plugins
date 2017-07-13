/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

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
    public void testUploadDirectoryWithRetries() throws Exception {
        final FTPUploader uploaderSpy = spy(ftpUploader);

        // Failure
        MojoExecutionException exception = null;
        doReturn(false)
                .when(uploaderSpy)
                .uploadDirectory(any(String.class), any(String.class), any(String.class), any(String.class),
                        any(String.class));
        try {
            uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password",
                    "sourceDir", "targetDir", 1);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }

        // Success
        doReturn(true)
                .when(uploaderSpy)
                .uploadDirectory(any(String.class), any(String.class), any(String.class), any(String.class),
                        any(String.class));
        uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password",
                "sourceDir", "targetDir", 1);
    }

    @Test
    public void testUploadDirectoryWithCredential() {

    }

    @Test
    public void testUploadDirectoryWithClient() {

    }

    @Test
    public void testUploadFile() {

    }
}
