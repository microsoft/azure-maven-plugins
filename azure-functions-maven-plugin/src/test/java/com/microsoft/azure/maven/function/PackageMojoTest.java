///**
// * Copyright (c) Microsoft Corporation. All rights reserved.
// * Licensed under the MIT License. See License.txt in the project root for
// * license information.
// */
//
//package com.microsoft.azure.maven.function;
//
//import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
//import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
//import org.apache.maven.execution.MavenSession;
//import org.apache.maven.project.MavenProject;
//import org.apache.maven.shared.filtering.MavenResourcesFiltering;
//import org.codehaus.plexus.util.ReflectionUtils;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.isNotNull;
//import static org.mockito.ArgumentMatchers.isNull;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.spy;
//
//@RunWith(MockitoJUnitRunner.class)
//public class PackageMojoTest extends MojoTestBase {
//    @Test
//    public void doExecute() throws Exception {
//        final PackageMojo mojo = getMojoFromPom();
//        final PackageMojo mojoSpy = spy(mojo);
//        final Set<Method> methods = new HashSet<>(Arrays.asList(this.getClass().getMethods()));
//        ReflectionUtils.setVariableValueInObject(mojoSpy, "finalName", "artifact-0.1.0");
//        doReturn(mock(AnnotationHandler.class)).when(mojoSpy).getAnnotationHandler();
//        doReturn(methods).when(mojoSpy).findAnnotatedMethods(any());
//        doReturn("target/azure-functions").when(mojoSpy).getDeploymentStagingDirectoryPath();
//        doReturn("target").when(mojoSpy).getBuildDirectoryAbsolutePath();
//        doReturn(mock(MavenProject.class)).when(mojoSpy).getProject();
//        doReturn(mock(MavenSession.class)).when(mojoSpy).getSession();
//        doReturn(false).when(mojoSpy).isInstallingExtensionNeeded(any());
//        doReturn(mock(MavenResourcesFiltering.class)).when(mojoSpy).getMavenResourcesFiltering();
//
//        mojoSpy.doExecute();
//    }
//
//    @Test
//    public void getAnnotationHandler() throws Exception {
//        final PackageMojo mojo = getMojoFromPom();
//        final AnnotationHandler handler = mojo.getAnnotationHandler();
//
//        assertNotNull(handler);
//        assertTrue(handler instanceof AnnotationHandlerImpl);
//    }
//
//    @Test
//    public void getScriptFilePath() throws Exception {
//        final PackageMojo mojo = getMojoFromPom();
//        final PackageMojo mojoSpy = spy(mojo);
//        ReflectionUtils.setVariableValueInObject(mojoSpy, "finalName", "artifact-0.1.0");
//
//        final String finalName = mojoSpy.getScriptFilePath();
//
//        assertEquals("../artifact-0.1.0.jar", finalName);
//    }
//
//    @Test
//    public void writeFunctionJsonFile() throws Exception {
//        final PackageMojo mojo = getMojoFromPom();
//        final PackageMojo mojoSpy = spy(mojo);
//        doReturn("target/azure-functions").when(mojoSpy).getDeploymentStagingDirectoryPath();
//        doNothing().when(mojoSpy).writeObjectToFile(isNull(), isNull(), isNotNull());
//
//        mojoSpy.writeFunctionJsonFile(null, "httpTrigger", null);
//    }
//
//    private PackageMojo getMojoFromPom() throws Exception {
//        final PackageMojo mojo = (PackageMojo) getMojoFromPom("/pom.xml", "package");
//        assertNotNull(mojo);
//        return mojo;
//    }
//}
