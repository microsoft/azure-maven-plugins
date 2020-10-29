/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class DeployMojoTest extends DeployMojo {

    @Test
    public void testCheckProjectPackaging() throws MojoExecutionException {
        final MavenProject mockProject = mock(MavenProject.class);

        doReturn("jar").when(mockProject).getPackaging();
        assertTrue(checkProjectPackaging(mockProject));

        doReturn("pom").when(mockProject).getPackaging();
        assertFalse(checkProjectPackaging(mockProject));

        doReturn("war").when(mockProject).getPackaging();
        try {
            checkProjectPackaging(mockProject);
            fail("Check project packaging should throw exception when project packing is war");
        } catch (MojoExecutionException exception) {
            // Should throw exception in this case
        }
    }

}
