/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.reflections.util.ClasspathHelper;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BuildMojoTest {
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
    public void doExecute() throws Exception {
        final BuildMojo mojo = getMojoFromPom("/pom.xml");
        assertNotNull(mojo);

        final BuildMojo mojoSpy = spy(mojo);
        final AnnotationHandler handler = mock(AnnotationHandler.class);
        doReturn(handler).when(mojoSpy).getAnnotationHandler();
        doReturn(ClasspathHelper.forPackage("com.microsoft.azure.maven.function.handlers").toArray()[0])
                .when(mojoSpy)
                .getClassUrl();
        doReturn("..\\\\test.jar").when(mojoSpy).getScriptFilePath();
        mojoSpy.doExecute();
    }

    @Test
    public void getAnnotationHandler() throws Exception {
        final BuildMojo mojo = getMojoFromPom("/pom.xml");
        assertNotNull(mojo);

        final AnnotationHandler handler = mojo.getAnnotationHandler();
        assertNotNull(handler);
        assertTrue(handler instanceof AnnotationHandlerImpl);
    }

    private BuildMojo getMojoFromPom(String filename) throws Exception {
        final File pom = new File(BuildMojoTest.class.getResource(filename).toURI());
        return (BuildMojo) rule.lookupMojo("build", pom);
    }
}
