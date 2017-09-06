/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import org.apache.maven.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployFacadeBaseImplTest extends DeployFacadeTestBase {
    private DeployFacadeBaseImpl facade = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(log).when(mojo).getLog();
        facade = new DeployFacadeBaseImpl(mojo) {
            @Override
            public DeployFacade setupRuntime() throws Exception {
                return this;
            }

            @Override
            public DeployFacade applySettings() throws Exception {
                return this;
            }

            @Override
            public DeployFacade commitChanges() throws Exception {
                return this;
            }
        };
        setupHandlerFactory();
    }

    @Test
    public void deployArtifactsWithNoResources() throws Exception {
        doReturn(null).when(mojo).getResources();

        facade.deployArtifacts();

        verify(log, times(1)).info(any(String.class));
    }

    @Test
    public void deployArtifactsWithResources() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);
        doReturn(getResourceList()).when(mojo).getResources();

        facadeSpy.deployArtifacts();

        verify(artifactHandler, times(1)).publish(ArgumentMatchers.<Resource>anyList());
        verifyNoMoreInteractions(artifactHandler);
    }

    @Test
    public void getMojo() throws Exception {
        assertSame(mojo, facade.getMojo());
    }

    private List<Resource> getResourceList() {
        final Resource resource = new Resource();
        resource.setDirectory("/");
        resource.setTargetPath("/");

        final List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        return resources;
    }
}
