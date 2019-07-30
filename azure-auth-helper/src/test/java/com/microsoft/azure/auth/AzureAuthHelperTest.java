/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, AzureAuthHelper.class})
public class AzureAuthHelperTest {

    @Test
    public void testOAuthLogin() throws Exception {
    }

    @Test
    public void testDeviceLogin() throws Exception {
    }

    @Test
    public void testRefreshTokenInvalidToken() throws Exception {
        try {
            AzureAuthHelper.refreshToken(AzureEnvironment.AZURE, "invalid");
            fail("Should throw AzureLoginFailureException when refreshToken is invalid.");
        } catch (ExecutionException e) {
            // ignore
        }
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
    public void testGetAzureSecretFile() throws Exception {
        final File azureSecretFile = AzureAuthHelper.getAzureSecretFile();
        assertEquals(Paths.get(System.getProperty("user.home"), ".azure", "azure-secret.json").toString(), azureSecretFile.getAbsolutePath());
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv("AZURE_CONFIG_DIR")).thenReturn("test_dir");
        assertEquals(Paths.get("test_dir", "azure-secret.json").toFile().getAbsolutePath(), AzureAuthHelper.getAzureSecretFile().getAbsolutePath());
    }

    @Test
    public void testWriteAzureCredentials() throws Exception {
    }

    @Test
    public void testReadAzureCredentials() throws Exception {
    }
}
