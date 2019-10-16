/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
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
        final AbstractAppServiceMojo mojoSpy = spy(mojo);
        final String pluginName = "azure-functions-maven-plugin";
        final String buildDirectoryAbsolutePath = "target";
        final String appName = "app";
        doReturn(pluginName).when(mojoSpy).getPluginName();
        doReturn(buildDirectoryAbsolutePath).when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(appName).when(mojoSpy).getAppName();

        final String stagingDirectoryPath = mojoSpy.getDeploymentStagingDirectoryPath();
        assertTrue(StringUtils.contains(stagingDirectoryPath, buildDirectoryAbsolutePath));
        assertTrue(StringUtils.contains(stagingDirectoryPath, "azure-functions"));
        assertTrue(StringUtils.contains(stagingDirectoryPath, appName));
    }
}
