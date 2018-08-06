/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAppServiceMojoTest {

    @InjectMocks
    private AbstractAppServiceMojo mojo = new AbstractAppServiceMojo() {
        @Override
        protected void doExecute() throws Exception {

        }
    };

    @Test
    public void getDeploymentStagingDirectory() {
        final AbstractAppServiceMojo spy = spy(mojo);
        final String pluginName = "azure-functions-maven-plugin";
        final String buildDirectoryAbsolutePath = "target";
        final String appName = "app";
        doReturn(pluginName).when(spy).getPluginName();
        doReturn(buildDirectoryAbsolutePath).when(spy).getBuildDirectoryAbsolutePath();
        doReturn(appName).when(spy).getAppName();

        assertEquals(
            spy.getDeploymentStagingDirectoryPath(),
            Paths.get(buildDirectoryAbsolutePath, "azure-functions", appName).toString()
        );
    }
}
