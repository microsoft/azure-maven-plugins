
/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.DeployMojo;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JarArtifactHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private JarArtifactHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new JarArtifactHandlerImpl(mojo);
    }

    @Test
    public void publish() throws Exception {
        // TODO: test if web.config was properly generated and uploaded
        
        final JarArtifactHandlerImpl handlerSpy = spy(handler);
        doNothing().when(handlerSpy).copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        doNothing().when(handlerSpy).uploadDirectoryToFTP();

        final List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource());
        doReturn(resourceList).when(mojo).getResources();
        handlerSpy.publish();
        verify(handlerSpy, times(1))
                .copyResourcesToStageDirectory(ArgumentMatchers.<Resource>anyList());
        verify(handlerSpy, times(1)).uploadDirectoryToFTP();
    }

}
