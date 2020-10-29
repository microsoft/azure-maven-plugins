/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.pom;

import com.microsoft.azure.maven.springcloud.TestHelper;
import com.microsoft.azure.maven.springcloud.config.ConfigurationUpdater;
import com.microsoft.azure.tools.springcloud.AppConfig;
import com.microsoft.azure.tools.springcloud.AppDeploymentConfig;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PomUtilsTest {
    @Test
    public void testSaveXml() throws Exception {
        final File pomFile = new File(this.getClass().getClassLoader().getResource("pom-4.xml").getFile());
        final File tempFile = Files.createTempFile("azure-spring-cloud-plugin-test", ".xml").toFile();
        Files.copy(pomFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        final Model model = TestHelper.readMavenModel(tempFile);
        final MavenProject project = mock(MavenProject.class);
        when(project.getModel()).thenReturn(model);
        when(project.getFile()).thenReturn(tempFile);
        final AppConfig app = new AppConfig();
        app.withSubscriptionId("subscriptionId1");
        app.withClusterName("clusterName1");
        app.withAppName("appName1");
        app.withPublic(true);

        final AppDeploymentConfig deploy = new AppDeploymentConfig();
        deploy.withCpu(1);
        deploy.withMemoryInGB(2);
        deploy.withInstanceCount(3);
        deploy.withRuntimeVersion("8");
        deploy.withJvmOptions("jvmOptions1");
        final Plugin plugin = model.getBuild().getPlugins().get(0);

        final PluginDescriptor pd = mock(PluginDescriptor.class);
        when(pd.getGroupId()).thenReturn(plugin.getGroupId());
        when(pd.getArtifactId()).thenReturn(plugin.getArtifactId());
        when(pd.getVersion()).thenReturn(plugin.getVersion());
        ConfigurationUpdater.updateAppConfigToPom(app.withDeployment(deploy), project, pd);
        final String updatedXml = String.join("\n", Files.readAllLines(tempFile.toPath(), Charset.defaultCharset()));
        assertTrue(updatedXml.contains("<maven.compiler.target>1.8\n" +
                "        </maven.compiler.target>"));
        assertTrue(updatedXml.contains("<configuration>\n" +
                "                    <subscriptionId>subscriptionId1</subscriptionId>\n" +
                "                    <clusterName>clusterName1</clusterName>\n" +
                "                    <appName>appName1</appName>\n" +
                "                    <isPublic>true</isPublic>\n" +
                "                    <deployment>\n" +
                "                        <cpu>1</cpu>\n" +
                "                        <memoryInGB>2</memoryInGB>\n" +
                "                        <instanceCount>3</instanceCount>\n" +
                "                        <jvmOptions>jvmOptions1</jvmOptions>\n" +
                "                        <runtimeVersion>8</runtimeVersion>\n" +
                "                        <resources>\n" +
                "                            <resource>\n" +
                "                                <filtering/>\n" +
                "                                <mergeId/>\n" +
                "                                <targetPath/>\n" +
                "                                <directory>${project.basedir}/target</directory>\n" +
                "                                <includes>\n" +
                "                                    <include>*.jar</include>\n" +
                "                                </includes>\n" +
                "                            </resource>\n" +
                "                        </resources>\n" +
                "                    </deployment>\n" +
                "                </configuration>"));
        tempFile.delete();
    }

    @Test
    public void testSaveXmlNoBuild() throws Exception {
        final File pomFile = new File(this.getClass().getClassLoader().getResource("pom-5.xml").getFile());
        final File tempFile = Files.createTempFile("azure-spring-cloud-plugin-test", ".xml").toFile();
        Files.copy(pomFile.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        final Model model = TestHelper.readMavenModel(tempFile);
        final MavenProject project = mock(MavenProject.class);
        when(project.getModel()).thenReturn(model);
        when(project.getFile()).thenReturn(tempFile);
        final AppConfig app = new AppConfig();
        app.withSubscriptionId("subscriptionId1");
        app.withClusterName("clusterName1");
        app.withAppName("appName1");
        app.withPublic(true);

        final AppDeploymentConfig deploy = new AppDeploymentConfig();
        deploy.withCpu(1);
        deploy.withMemoryInGB(2);
        deploy.withInstanceCount(3);
        deploy.withRuntimeVersion("8");
        deploy.withJvmOptions("jvmOptions1");

        final PluginDescriptor pd = mock(PluginDescriptor.class);
        when(pd.getGroupId()).thenReturn("com.microsoft.azure");
        when(pd.getArtifactId()).thenReturn("azure-spring-cloud-maven-plugin");
        when(pd.getVersion()).thenReturn("0.1.0.SNAPSHOT");
        ConfigurationUpdater.updateAppConfigToPom(app.withDeployment(deploy), project, pd);
        final String updatedXml = String.join("\n", Files.readAllLines(tempFile.toPath(), Charset.defaultCharset()));
        assertTrue(updatedXml.contains("<maven.compiler.target>1.8\n" +
                "        </maven.compiler.target>"));
        assertTrue(updatedXml.contains("<configuration>\n" +
                "                    <subscriptionId>subscriptionId1</subscriptionId>\n" +
                "                    <clusterName>clusterName1</clusterName>\n" +
                "                    <appName>appName1</appName>\n" +
                "                    <isPublic>true</isPublic>\n" +
                "                    <deployment>\n" +
                "                        <cpu>1</cpu>\n" +
                "                        <memoryInGB>2</memoryInGB>\n" +
                "                        <instanceCount>3</instanceCount>\n" +
                "                        <jvmOptions>jvmOptions1</jvmOptions>\n" +
                "                        <runtimeVersion>8</runtimeVersion>\n" +
                "                        <resources>\n" +
                "                            <resource>\n" +
                "                                <filtering/>\n" +
                "                                <mergeId/>\n" +
                "                                <targetPath/>\n" +
                "                                <directory>${project.basedir}/target</directory>\n" +
                "                                <includes>\n" +
                "                                    <include>*.jar</include>\n" +
                "                                </includes>\n" +
                "                            </resource>\n" +
                "                        </resources>\n" +
                "                    </deployment>\n" +
                "                </configuration>"));
        tempFile.delete();
    }
}
