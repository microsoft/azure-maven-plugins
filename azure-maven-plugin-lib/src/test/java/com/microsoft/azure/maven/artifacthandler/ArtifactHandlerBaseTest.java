/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactHandlerBaseTest {
    private final AbstractAppServiceMojo mojo = spy(AbstractAppServiceMojo.class);

    private final ArtifactHandlerBase baseClass = new ArtifactHandlerBase<AbstractAppServiceMojo>(mojo) {
        @Override
        public void publish(DeployTarget deployTarget) {
            // do nothing
        }
    };

    @Test
    public void prepareResources() throws IOException, MojoExecutionException {
        final ArtifactHandlerBase baseClassSpy = spy(baseClass);
        final List<Resource> resourceList = new ArrayList<>();
        doReturn(mock(MavenProject.class)).when(mojo).getProject();
        doReturn(mock(MavenSession.class)).when(mojo).getSession();
        doReturn(mock(MavenResourcesFiltering.class)).when(mojo).getMavenResourcesFiltering();
        resourceList.add(new Resource());
        doReturn(resourceList).when(mojo).getResources();
        doReturn("").when(mojo).getDeploymentStagingDirectoryPath();

        baseClassSpy.prepareResources();

        verify(baseClassSpy, times(1)).prepareResources();
        verifyNoMoreInteractions(baseClassSpy);
    }

    @Test(expected = MojoExecutionException.class)
    public void prepareResourcesThrowException() throws IOException, MojoExecutionException {
        final ArtifactHandlerBase baseClassSpy = spy(baseClass);
        baseClassSpy.prepareResources();
    }

    @Test(expected = MojoExecutionException.class)
    public void assureStagingDirectoryNotEmptyThrowException() throws MojoExecutionException {
        doReturn("").when(mojo).getDeploymentStagingDirectoryPath();

        baseClass.assureStagingDirectoryNotEmpty();
    }

    @Test
    public void assureStagingDirectoryNotEmpty() throws MojoExecutionException {
        final ArtifactHandlerBase baseClassSpy = spy(baseClass);
        doNothing().when(baseClassSpy).assureStagingDirectoryNotEmpty();

        baseClassSpy.assureStagingDirectoryNotEmpty();

        verify(baseClassSpy, times(1)).assureStagingDirectoryNotEmpty();
        verifyNoMoreInteractions(baseClassSpy);
    }
}
