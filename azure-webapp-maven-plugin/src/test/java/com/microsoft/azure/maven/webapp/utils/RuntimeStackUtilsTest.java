/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.appservice.RuntimeStack;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class RuntimeStackUtilsTest {

    @Test
    public void getRuntimeStackFromString() {
        assertEquals(RuntimeStackUtils.getRuntimeStack("jre8", "tomcat 8.5"), RuntimeStack.TOMCAT_8_5_JRE8);
        assertEquals(RuntimeStackUtils.getRuntimeStack("java11", "TOMCAT 9.0"), RuntimeStack.TOMCAT_9_0_JAVA11);
        assertEquals(RuntimeStackUtils.getRuntimeStack("java11", null), RuntimeStack.JAVA_11_JAVA11);
        assertEquals(RuntimeStackUtils.getRuntimeStack("jre8", "jre8"), RuntimeStack.JAVA_8_JRE8);
    }

    @Test
    public void getWebContainerFromRuntimeStack() {
        assertEquals(RuntimeStackUtils.getWebContainerFromRuntimeStack(RuntimeStack.TOMCAT_8_5_JAVA11), "Tomcat 8.5");
        assertEquals(RuntimeStackUtils.getWebContainerFromRuntimeStack(RuntimeStack.JAVA_8_JRE8), "Java SE");
        assertEquals(RuntimeStackUtils.getWebContainerFromRuntimeStack(RuntimeStack.JAVA_11_JAVA11), "Java SE");
    }
}
