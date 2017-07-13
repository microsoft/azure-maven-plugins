/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FTPArtifactHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private FTPArtifactHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new FTPArtifactHandlerImpl(mojo);
    }

    @Test
    public void publish() throws Exception {
        final FTPArtifactHandlerImpl handlerSpy = spy(handler);
        doNothing().when(handlerSpy).copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        doNothing().when(handlerSpy).uploadDirectoryToFTP();

        handlerSpy.publish(new ArrayList<Resource>());
        verify(handlerSpy, times(1))
                .copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        verify(handlerSpy, times(1)).uploadDirectoryToFTP();
    }

    @Test
    public void copyResourcesToStageDirectory() throws Exception {
    }

    @Test
    public void uploadDirectoryToFTP() throws Exception {
    }
}
