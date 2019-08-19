/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.credentials.MSICredentials;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment(" "));
        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("azure"));
        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("aZUre"));

        assertEquals(AzureEnvironment.AZURE, AzureAuthHelper.getAzureEnvironment("AZURE_CLOUD"));

        assertEquals(AzureEnvironment.AZURE_GERMANY, AzureAuthHelper.getAzureEnvironment("AZURE_GERMANY"));
        assertEquals(AzureEnvironment.AZURE_GERMANY, AzureAuthHelper.getAzureEnvironment("AzureGermanCloud"));
        assertEquals(AzureEnvironment.AZURE_GERMANY, AzureAuthHelper.getAzureEnvironment("AZUREGERMANCLOUD"));

        assertEquals(AzureEnvironment.AZURE_US_GOVERNMENT, AzureAuthHelper.getAzureEnvironment("AZURE_US_GOVERNMENT"));
        assertEquals(AzureEnvironment.AZURE_US_GOVERNMENT, AzureAuthHelper.getAzureEnvironment("AZUREUSGOVERNMENT"));

        assertEquals(AzureEnvironment.AZURE_CHINA, AzureAuthHelper.getAzureEnvironment("AzureChinaCloud"));
        assertEquals(AzureEnvironment.AZURE_CHINA, AzureAuthHelper.getAzureEnvironment("Azure_China"));

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
    public void testWriteAzureCredentials() throws Exception {
        final File testConfigDir = new File(this.getClass().getResource("/azure-login/azure-secret.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        assertTrue(AzureAuthHelper.existsAzureSecretFile());
        final AzureCredential cred = AzureAuthHelper.readAzureCredentials();
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File tempFile = new File(tempDirectory, "azure-secret.json");
        AzureAuthHelper.writeAzureCredentials(cred, tempFile);
        final AzureCredential cred2 = AzureAuthHelper.readAzureCredentials(tempFile);
        assertEquals(cred.getAccessToken(), cred2.getAccessToken());
        assertEquals(cred.getAccessTokenType(), cred2.getAccessTokenType());
        assertEquals(cred.getRefreshToken(), cred2.getRefreshToken());
        assertEquals(cred.getDefaultSubscription(), cred2.getDefaultSubscription());
        assertEquals(cred.getEnvironment(), cred2.getEnvironment());
        assertEquals(cred.getIdToken(), cred2.getIdToken());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, tempDirectory.getAbsolutePath());
        assertTrue(AzureAuthHelper.existsAzureSecretFile());
        AzureAuthHelper.deleteAzureSecretFile();
        assertFalse(AzureAuthHelper.existsAzureSecretFile());
        tempDirectory.delete();

        assertFalse(AzureAuthHelper.deleteAzureSecretFile());
    }

    @Test
    public void testWriteAzureCredentialsBadParameter() throws Exception {
        final File tempFile = File.createTempFile("azure-auth-helper-test", ".json");
        try {
            AzureAuthHelper.writeAzureCredentials(null, tempFile);
            fail("Should throw IAE here.");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            AzureAuthHelper.writeAzureCredentials(AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult()), null);
            fail("Should throw IAE here.");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testIsInCloudShell() {
        TestHelper.injectEnvironmentVariable(Constants.CLOUD_SHELL_ENV_KEY, "azure");
        assertTrue(AzureAuthHelper.isInCloudShell());
        TestHelper.injectEnvironmentVariable(Constants.CLOUD_SHELL_ENV_KEY, null);
        assertFalse(AzureAuthHelper.isInCloudShell());
    }

    @Test
    public void testGetAzureTokenCredentials() throws Exception {
        // 1. use azure-secret.json
        File testConfigDir = new File(this.getClass().getResource("/azure-login/azure-secret.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        AzureTokenCredentials cred = AzureAuthHelper.getAzureTokenCredentials(null);
        assertNotNull(cred);
        assertEquals("00000000-0000-0000-0000-000000000001", cred.defaultSubscriptionId());

        // 2. use azure cli(non SP)
        testConfigDir = new File(this.getClass().getResource("/azure-cli/default/azureProfile.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        cred = AzureAuthHelper.getAzureTokenCredentials(null);
        assertNotNull(cred);
        assertTrue(cred instanceof AzureCliCredentials);
        final AzureCliCredentials cliCred = (AzureCliCredentials) cred;
        assertEquals("00000000-0000-0000-0000-000000000001", cliCred.defaultSubscriptionId());
        assertEquals("00000000-0000-0000-0000-000000000002", cliCred.clientId());
        assertEquals("00000000-0000-0000-0000-000000000003", cliCred.domain());
        assertEquals(AzureEnvironment.AZURE_CHINA, cliCred.environment());

        // 3. use azure cli(SP)
        testConfigDir = new File(this.getClass().getResource("/azure-cli/sp/azureProfile.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        cred = AzureAuthHelper.getAzureTokenCredentials(null);
        assertNotNull(cred);
        assertTrue(cred instanceof ApplicationTokenCredentials);
        final ApplicationTokenCredentials applicationTokenCredentials = (ApplicationTokenCredentials) cred;
        assertEquals("00000000-0000-0000-0000-000000000001", cred.defaultSubscriptionId());
        assertEquals("00000000-0000-0000-0000-000000000002", applicationTokenCredentials.clientId());
        assertEquals("00000000-0000-0000-0000-000000000003", cred.domain());
        assertEquals(AzureEnvironment.AZURE_CHINA, cliCred.environment());

        // 4. use cloud shell
        TestHelper.injectEnvironmentVariable(Constants.CLOUD_SHELL_ENV_KEY, "azure");
        assertTrue(AzureAuthHelper.isInCloudShell());
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "non-exist-folder");
        cred = AzureAuthHelper.getAzureTokenCredentials(null);
        assertNotNull(cred);
        assertTrue(cred instanceof MSICredentials);

        // 5. all of the ways have been tried
        TestHelper.injectEnvironmentVariable(Constants.CLOUD_SHELL_ENV_KEY, null);
        assertNull(AzureAuthHelper.getAzureTokenCredentials(null));
    }

    @Test
    public void testGetAzureTokenCredentialsFromConfiguration() throws Exception {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setClient("client_id");
        auth.setTenant("tenant_id");
        auth.setKey("key");
        auth.setEnvironment("azure_germany");
        final AzureTokenCredentials cred = AzureAuthHelper.getAzureTokenCredentials(auth);
        assertNotNull(cred);
        assertTrue(cred instanceof ApplicationTokenCredentials);
        assertNull(cred.defaultSubscriptionId());
        assertEquals(AzureEnvironment.AZURE_GERMANY, cred.environment());
        assertEquals("tenant_id", cred.domain());
        assertEquals("client_id", ((ApplicationTokenCredentials) cred).clientId());
    }

    @Test
    public void testGetToken() throws Exception {
        @SuppressWarnings("serial")
        final AzureEnvironment testEnvironment = new AzureEnvironment(new HashMap<String, String>() {{
                put(AzureEnvironment.Endpoint.ACTIVE_DIRECTORY.toString(), "https://xxxxxxx/");
                put(AzureEnvironment.Endpoint.MANAGEMENT.toString(), "https://xxxxxxx/");
                put(AzureEnvironment.Endpoint.RESOURCE_MANAGER.toString(), "https://xxxxxxx/");
            }});

        final AzureCredential cred = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        // sample token from https://medium.com/@siddharthac6/json-web-token-jwt-the-right-way-of-implementing-with-node-js-65b8915d550e
        final String EXPIRED_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJkYXRhMSI6IkRhdGEgMSIsImRhdGEyIjoiRGF0YSAyIiwiZGF0YTMiOiJEYXRhIDMiLCJkYXRhNCI6Ik" +
                "RhdGEgNCIsImlhdCI6MTUyNTE5MzM3NywiZXhwIjoxNTI1MjM2NTc3LCJhdWQiOiJodHRwOi8vbXlzb2Z0Y29ycC5pbiIsImlzcyI6Ik15c29mdCBjb3JwIiwic3ViIjoic29tZUB1c2" +
                "VyLmNvbSJ9.ID2fn6t0tcoXeTgkG2AivnG1skctbCAyY8M1ZF38kFvUJozRWSbdVc7FLwot-bwV8k1imV8o0fqdv5sVY0Yzmg";
        cred.setAccessToken(EXPIRED_TOKEN);
        try {
            AzureAuthHelper.getMavenAzureLoginCredentials(cred, testEnvironment).getToken(testEnvironment.resourceManagerEndpoint());
            fail("should throw UnknownHostException when refreshing the access token.");
        } catch (UnknownHostException ex) {
            // expected
        }

        // token created by site: http://jwtbuilder.jamiekurtz.com/,
        //{
        //    "iss": "Online JWT Builder",
        //    "iat": 1566201321,
        //    "exp": 4090722921,
        //    "aud": "www.example.com",
        //    "sub": "jrocket@example.com",
        //    "GivenName": "Johnny",
        //    "Surname": "Rocket",
        //    "Email": "jrocket@example.com",
        //    "Role": [
        //        "Manager",
        //        "Project Administrator"
        //    ]
        //}
        final String VALID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1NjYyMDEzMjEsImV4cCI6NDA5MDcyMjkyMSwiYXVkIjoid3d3" +
                "LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhh" +
                "bXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.m0yY1rRd9-zo9pUKWdkuuCSIj48K-X8IPr1-3gj-dGQ";

        cred.setAccessToken(VALID_TOKEN);
        assertEquals(VALID_TOKEN,  AzureAuthHelper.getMavenAzureLoginCredentials(cred, testEnvironment).getToken(testEnvironment.resourceManagerEndpoint()));

    }
}
