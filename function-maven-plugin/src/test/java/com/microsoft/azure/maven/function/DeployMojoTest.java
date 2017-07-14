/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    @Test
    public void getConfiguration() throws Exception {
        final DeployMojo mojo = getMojoFromPom("/pom.xml");
        assertNotNull(mojo);

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("a-function", mojo.getFunctionName());

        assertEquals("westeurope", mojo.getRegion());

        assertEquals("function-maven-plugin", mojo.getPluginName());

        assertEquals(1, mojo.getResources().size());
    }

    private DeployMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(DeployMojoTest.class.getResource(filename).toURI());
        return (DeployMojo) rule.lookupMojo("deploy", pom);
    }
}
