/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import com.microsoft.azure.maven.spring.TestHelper;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MavenUtilsTest {
    @Test
    public void testPluginConfigurationFromBuild() throws Exception {
        final File pom = new File(this.getClass().getClassLoader().getResource("pom-1.xml").getFile());
        final Model model = TestHelper.readMavenModel(pom);
        final MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getModel()).thenReturn(model);
        final Xpp3Dom config = MavenUtils.getPluginConfiguration(project, "com.microsoft.azure:azure-spring-cloud-maven-plugin");
        assertNotNull(config);
        assertNotNull(config.getChild("public"));
        assertEquals("true", config.getChild("public").getValue());
        assertNotNull(config.getChild("deployment"));
        assertEquals("1", config.getChild("deployment").getChild("cpu").getValue());
    }

    @Test
    public void testPluginConfigurationFromPluginManagement() throws Exception {
        final File pom = new File(this.getClass().getClassLoader().getResource("pom-2.xml").getFile());
        final Model model = TestHelper.readMavenModel(pom);
        final MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getModel()).thenReturn(model);
        final Xpp3Dom config = MavenUtils.getPluginConfiguration(project, "com.microsoft.azure:azure-spring-cloud-maven-plugin");
        assertNotNull(config);
        assertNotNull(config.getChild("public"));
        assertEquals("false", config.getChild("public").getValue());
        assertNotNull(config.getChild("deployment"));
        assertEquals("2", config.getChild("deployment").getChild("cpu").getValue());
    }

    @Test
    public void testNoPluginConfigurationFromBuild() throws Exception {
        final File pom = new File(this.getClass().getClassLoader().getResource("pom-3.xml").getFile());
        final Model model = TestHelper.readMavenModel(pom);
        final MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.when(project.getModel()).thenReturn(model);
        final Xpp3Dom config = MavenUtils.getPluginConfiguration(project, "com.microsoft.azure:azure-spring-cloud-maven-plugin");
        assertNull(config);
    }
}
