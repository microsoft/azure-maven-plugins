/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AzureAuthHelperTest {
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
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "");
    }

    @Test
    public void testGetAzureConfigFolder() throws Exception {
        final File azureConfigFolder = AzureAuthHelper.getAzureConfigFolder();
        assertEquals(Paths.get(System.getProperty("user.home"), ".azure").toString(), azureConfigFolder.getAbsolutePath());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "test_dir");
        assertEquals(Paths.get("test_dir").toFile().getAbsolutePath(), AzureAuthHelper.getAzureConfigFolder().getAbsolutePath());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "");
    }

}
