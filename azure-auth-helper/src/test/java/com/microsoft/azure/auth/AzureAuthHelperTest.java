/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AzureAuthHelperTest {

    @After
    public void afterEachTestMethod() {
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, null);
    }

    @Test
    public void testRefreshTokenInvalidParameter() throws Exception {
        try {
            AzureAuthHelper.refreshToken(null, "abc");
            fail("Should throw IAE when env is null.");
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            AzureAuthHelper.refreshToken(AzureEnvironment.AZURE_CHINA, "");
            fail("Should throw IAE when refreshToken is empty.");
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Test
    public void testGetAzureEnvironment() {
        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment(null));
        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("azure"));
        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("aZUre"));

        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("AZURE_CLOUD"));

        assertEquals(AzureEnvironment.AZURE_GERMANY, AzureAuthHelper.getAzureEnvironment("AZURE_GERMANY"));
        assertEquals(AzureEnvironment.AZURE_GERMANY, AzureAuthHelper.getAzureEnvironment("AzureGermanCloud"));
        assertEquals(AzureEnvironment.AZURE_GERMANY, AzureAuthHelper.getAzureEnvironment("AZUREGERMANCLOUD"));

        assertEquals(AzureEnvironment.AZURE_US_GOVERNMENT, AzureAuthHelper.getAzureEnvironment("AZURE_US_GOVERNMENT"));
        assertEquals(AzureEnvironment.AZURE_CHINA, AzureAuthHelper.getAzureEnvironment("AzureChinaCloud"));

        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("AzureChinaCloud "));
    }

    @Test
    public void testEnvironmentValidation() {
        assertTrue(AzureAuthHelper.validateEnvironment(null));
        assertTrue(AzureAuthHelper.validateEnvironment(""));
        assertTrue(AzureAuthHelper.validateEnvironment(" "));
        assertTrue(AzureAuthHelper.validateEnvironment("azure"));
        assertFalse(AzureAuthHelper.validateEnvironment("azure "));
        assertTrue(AzureAuthHelper.validateEnvironment("azure_cloud"));
        assertTrue(AzureAuthHelper.validateEnvironment("AZURE_CLOUD"));
        assertTrue(AzureAuthHelper.validateEnvironment("aZURe"));
        assertFalse(AzureAuthHelper.validateEnvironment("aZURe "));
        assertFalse(AzureAuthHelper.validateEnvironment("foo"));

    }

    @Test
    public void testGetAzureSecretFile() throws Exception {
        final File azureSecretFile = AzureAuthHelper.getAzureSecretFile();
        assertEquals(Paths.get(System.getProperty("user.home"), ".azure", "azure-secret.json").toString(), azureSecretFile.getAbsolutePath());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "test_dir");
        assertEquals(Paths.get("test_dir", "azure-secret.json").toFile().getAbsolutePath(), AzureAuthHelper.getAzureSecretFile().getAbsolutePath());
    }

    @Test
    public void testGetAzureConfigFolder() throws Exception {
        final File azureConfigFolder = AzureAuthHelper.getAzureConfigFolder();
        assertEquals(Paths.get(System.getProperty("user.home"), ".azure").toString(), azureConfigFolder.getAbsolutePath());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "test_dir");
        assertEquals(Paths.get("test_dir").toFile().getAbsolutePath(), AzureAuthHelper.getAzureConfigFolder().getAbsolutePath());
    }

    @Test
    public void testExistsAzureSecretFile() {
        final File testConfigDir = new File(this.getClass().getResource("/azure-login/azure-secret.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        assertTrue(AzureAuthHelper.existsAzureSecretFile());

        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getParentFile().getAbsolutePath());
        assertFalse(AzureAuthHelper.existsAzureSecretFile());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "");
    }

    @Test
    public void testReadAzureCredentials() throws Exception {
        final File testConfigDir = new File(this.getClass().getResource("/azure-login/azure-secret.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        assertTrue(AzureAuthHelper.existsAzureSecretFile());
        assertNotNull(AzureAuthHelper.readAzureCredentials());

        try {
            AzureAuthHelper.readAzureCredentials(null);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expect
        }
    }

    @Test
    public void testIsInCloudShell() {
        TestHelper.injectEnvironmentVariable(Constants.CLOUD_SHELL_ENV_KEY, "azure");
        assertTrue(AzureAuthHelper.isInCloudShell());
        TestHelper.injectEnvironmentVariable(Constants.CLOUD_SHELL_ENV_KEY, null);
        assertFalse(AzureAuthHelper.isInCloudShell());
    }
}
