/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class LogoutMojoTest {
    @Rule
    public MojoRule rule = new MojoRule();

    private LogoutMojo mojo;

    @Before
    public void setUp() throws Exception {
        final File pom = new File(this.getClass().getResource("/maven/projects/simple/pom.xml").getFile());
        assertNotNull(pom);
        assertTrue(pom.exists());
        mojo = (LogoutMojo) rule.lookupMojo("logout", pom);
        final MojoExecution execution = rule.newMojoExecution("logout");
        assertNotNull(mojo);
        mojo.plugin = execution.getMojoDescriptor().getPluginDescriptor();
    }

    @Test
    public void testDeleteAzureFile() throws Exception {
        final File testConfigDir = new File(this.getClass().getResource("/auth/azure-secret.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        assertTrue(AzureAuthHelper.existsAzureSecretFile());
        final File tempDirectory = Files.createTempDirectory("azure-plugin-test").toFile();
        final File tempFile = new File(tempDirectory, "azure-secret.json");
        FileUtils.copyFile(AzureAuthHelper.getAzureSecretFile(), tempFile);
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, tempDirectory.getAbsolutePath());
        assertTrue(AzureAuthHelper.existsAzureSecretFile());
        mojo.isTelemetryAllowed = true;
        mojo.execute();
        assertFalse(AzureAuthHelper.existsAzureSecretFile());
    }

    @Test
    public void testLogoutBeforeLogin() throws Exception {
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "non-exist-config-root");
        assertFalse(AzureAuthHelper.existsAzureSecretFile());
        mojo.isTelemetryAllowed = false;
        mojo.execute();
    }

}
