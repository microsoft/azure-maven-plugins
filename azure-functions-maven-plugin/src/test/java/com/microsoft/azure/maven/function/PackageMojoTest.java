/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.reflections.util.ClasspathHelper;

import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;

@RunWith(MockitoJUnitRunner.class)
public class PackageMojoTest extends MojoTestBase {
    @Test
    public void doExecute() throws Exception {
        final PackageMojo mojo = getMojoFromPom();
        final PackageMojo mojoSpy = spy(mojo);
        ReflectionUtils.setVariableValueInObject(mojoSpy, "finalName", "artifact-0.1.0");
        doReturn(mock(AnnotationHandler.class)).when(mojoSpy).getAnnotationHandler();
        doReturn(ClasspathHelper.forPackage("com.microsoft.azure.maven.function.handlers").toArray()[0])
                .when(mojoSpy)
                .getTargetClassUrl();
        doReturn("target/azure-functions").when(mojoSpy).getDeploymentStageDirectory();
        doReturn("target").when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(mock(MavenProject.class)).when(mojoSpy).getProject();
        doReturn(mock(MavenSession.class)).when(mojoSpy).getSession();
        doReturn(mock(MavenResourcesFiltering.class)).when(mojoSpy).getMavenResourcesFiltering();

        mojoSpy.doExecute();
    }

    @Test
    public void getAnnotationHandler() throws Exception {
        final PackageMojo mojo = getMojoFromPom();
        final AnnotationHandler handler = mojo.getAnnotationHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof AnnotationHandlerImpl);
    }

    @Test
    public void getScriptFilePath() throws Exception {
        final PackageMojo mojo = getMojoFromPom();
        final PackageMojo mojoSpy = spy(mojo);
        ReflectionUtils.setVariableValueInObject(mojoSpy, "finalName", "artifact-0.1.0");

        final String finalName = mojoSpy.getScriptFilePath();

        assertEquals(Paths.get("..", "artifact-0.1.0.jar").toString(), finalName);
    }

    @Test
    public void writeFunctionJsonFile() throws Exception {
        final PackageMojo mojo = getMojoFromPom();
        final PackageMojo mojoSpy = spy(mojo);
        doReturn("target/azure-functions").when(mojoSpy).getDeploymentStageDirectory();
        doNothing().when(mojoSpy).writeObjectToFile(isNull(), isNull(), isNotNull());

        mojoSpy.writeFunctionJsonFile(null, "httpTrigger", null);
    }

    private PackageMojo getMojoFromPom() throws Exception {
        final PackageMojo mojo = (PackageMojo) getMojoFromPom("/pom.xml", "package");
        assertNotNull(mojo);
        return mojo;
    }
}
