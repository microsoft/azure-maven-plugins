/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.appservice;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAppServiceMojoTest {

    @InjectMocks
    private AbstractAppServiceMojo mojo = new AbstractAppServiceMojo() {
        @Override
        protected void doExecute() throws AzureExecutionException {

        }
    };

    @Test
    public void getDeploymentStagingDirectory() {
        final AbstractAppServiceMojo mojoSpy = Mockito.spy(mojo);
        final String pluginName = "azure-functions-maven-plugin";
        final String buildDirectoryAbsolutePath = "target";
        final String appName = "app";
        Mockito.doReturn(pluginName).when(mojoSpy).getPluginName();
        Mockito.doReturn(buildDirectoryAbsolutePath).when(mojoSpy).getBuildDirectoryAbsolutePath();
        Mockito.doReturn(appName).when(mojoSpy).getAppName();

        assertEquals(
            mojoSpy.getDeploymentStagingDirectoryPath(),
            Paths.get(buildDirectoryAbsolutePath, "azure-functions", appName).toString()
        );
    }
}
