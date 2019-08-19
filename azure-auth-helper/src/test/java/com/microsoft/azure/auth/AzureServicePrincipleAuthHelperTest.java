/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AzureServicePrincipleAuthHelperTest {
    @Test
    public void testGetSPCredentialsBadParameter() throws Exception {
        final AuthConfiguration config = new AuthConfiguration();
        try {
            AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(config);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            config.setClient("client_id");
            AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(config);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            config.setTenant("tenant_id");
            AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(config);
            fail("Should throw exception when there is no key and no certificate");
        } catch (InvalidConfigurationException ex) {
            // expected
        }

        try {
            config.setKey("key");
            assertNotNull(AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(config));
        } catch (IllegalArgumentException ex) {
            // expected
            fail("Should not throw IAE");
        }

        try {
            config.setKey(null);
            config.setCertificate(this.getClass().getResource("/test.pem").getFile());
            assertNotNull(AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(config));

            config.setCertificatePassword("pass1");
            assertNotNull(AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(config));
        } catch (IllegalArgumentException ex) {
            // expected
            fail("Should not throw IAE");
        }
    }

    @Test
    public void testGetSPCredentials() throws Exception {
        final File testConfigDir = new File(this.getClass().getResource("/azure-cli/azureProfile.json").getFile()).getParentFile();
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, testConfigDir.getAbsolutePath());
        final ApplicationTokenCredentials cred = AzureServicePrincipleAuthHelper.getCredentialFromAzureCliWithServicePrincipal();
        assertEquals("00000000-0000-0000-0000-000000000002", cred.clientId());
        assertEquals("https://management.chinacloudapi.cn/", cred.environment().resourceManagerEndpoint());
        assertEquals("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX", TestHelper.readField(cred, "clientSecret"));
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, null);

    }
}
