/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactHandlerBaseTest {
    private final AbstractAppServiceMojo mojo = mock(AbstractAppServiceMojo.class);

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

    private void buildHandler() {
        handler = builder.project(mojo.getProject())
            .session(mojo.getSession())
            .filtering(mojo.getMavenResourcesFiltering())
            .resources(mojo.getResources())
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .build();
    }

    @Test
    public void testCtror() throws IOException, AzureExecutionException {
        final List<Resource> resourceList = new ArrayList<>();
        doReturn(mock(MavenProject.class)).when(mojo).getProject();
        doReturn(mock(MavenSession.class)).when(mojo).getSession();
        doReturn(mock(MavenResourcesFiltering.class)).when(mojo).getMavenResourcesFiltering();
        resourceList.add(new Resource());
        doReturn(resourceList).when(mojo).getResources();
        doReturn("target/classes").when(mojo).getDeploymentStagingDirectoryPath();

        buildHandler();

        assertEquals(handler.project, mojo.getProject());
        assertEquals(handler.session, mojo.getSession());
        assertEquals(handler.resources, mojo.getResources());
        assertEquals(handler.filtering, mojo.getMavenResourcesFiltering());
        assertEquals(handler.stagingDirectoryPath, mojo.getDeploymentStagingDirectoryPath());
    }
}
