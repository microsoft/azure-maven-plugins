/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactHandlerBaseTest {
    private ArtifactHandlerBase.Builder builder = new ArtifactHandlerBase.Builder() {
        @Override
        protected ArtifactHandlerBase.Builder self() {
            return this;
        }

        @Override
        public ArtifactHandlerBase build() {
            return new ArtifactHandlerBase(builder) {
                @Override
                public void publish(DeployTarget deployTarget) {
                    // nothing
                }
            };
        }
    };

    private ArtifactHandlerBase handler;
    private ArtifactHandlerBase handlerSpy;

    private void buildHandler() {
        handler = builder.project(mock(IProject.class))
            .stagingDirectoryPath("target/classes")
            .build();
        handlerSpy = spy(handler);
    }

    @Test
    public void testCtor() throws IOException, AzureExecutionException {
        buildHandler();
        assertEquals(handler.stagingDirectoryPath, "target/classes");
    }

    @Test(expected = AzureExecutionException.class)
    public void assureStagingDirectoryNotEmptyThrowException() throws AzureExecutionException {
        handler = builder.project(mock(IProject.class))
            .stagingDirectoryPath("")
            .build();
        handler.assureStagingDirectoryNotEmpty();
    }

    @Test
    public void assureStagingDirectoryNotEmpty() throws AzureExecutionException {
        buildHandler();
        doNothing().when(handlerSpy).assureStagingDirectoryNotEmpty();

        handlerSpy.assureStagingDirectoryNotEmpty();

        verify(handlerSpy, times(1)).assureStagingDirectoryNotEmpty();
        verifyNoMoreInteractions(handlerSpy);
    }
}
