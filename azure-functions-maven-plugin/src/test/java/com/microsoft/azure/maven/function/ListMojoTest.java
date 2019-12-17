///**
// * Copyright (c) Microsoft Corporation. All rights reserved.
// * Licensed under the MIT License. See License.txt in the project root for
// * license information.
// */
//
//package com.microsoft.azure.maven.function;
//
//import org.apache.maven.plugin.logging.Log;
//import org.junit.Test;
//
//import static org.junit.Assert.assertNotNull;
//import static org.mockito.Mockito.anyString;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.verify;
//
//public class ListMojoTest extends MojoTestBase {
//
//    @Test
//    public void doExecute() throws Exception {
//        final ListMojo mojo = getMojoFromPom();
//        final ListMojo mojoSpy = spy(mojo);
//        final Log log = mock(Log.class);
//
//        doReturn(log).when(mojoSpy).getLog();
//        doNothing().when(log).info(anyString());
//        doNothing().when(mojoSpy).printToSystemOut(anyString());
//
//        mojoSpy.doExecute();
//
//        verify(log).info(ListH.TEMPLATES_START);
//        verify(log).info(ListMojo.TEMPLATES_END);
//        verify(log).info(ListMojo.BINDINGS_START);
//        verify(log).info(ListMojo.BINDINGS_END);
//        verify(log).info(ListMojo.RESOURCES_START);
//        verify(log).info(ListMojo.RESOURCES_END);
//    }
//
//    private ListMojo getMojoFromPom() throws Exception {
//        final ListMojo mojo = (ListMojo) getMojoFromPom("/pom.xml", "list");
//        assertNotNull(mojo);
//        return mojo;
//    }
//}
