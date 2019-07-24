/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        } catch (AzureLoginFailureException e) {
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
    public void tetGetAzureSecretFile() throws Exception {
        File azureSecretFile = AzureAuthHelper.getAzureSecretFile();
        assertEquals(Paths.get(System.getProperty(Constants.USER_HOME_KEY), ".azure", "azure-secret.json").toString(),
                azureSecretFile.getAbsolutePath());
        System.setProperty(Constants.AZURE_HOME_KEY, "test_dir");
        azureSecretFile = AzureAuthHelper.getAzureSecretFile();
        assertEquals(Paths.get("test_dir", "azure-secret.json").toFile().getAbsolutePath(),
                azureSecretFile.getAbsolutePath());
    }

    @Test
    public void testWriteAzureCredentials() throws Exception {
    }

    @Test
    public void testReadAzureCredentials() throws Exception {
    }

    @Test
    public void testAuthorizationUrl() throws Exception {
        String url = AzureAuthHelper.authorizationUrl(AzureEnvironment.AZURE, "http://localhost:4663");
        Map<String, String> queryMap = splitQuery(url);
        assertEquals(Constants.CLIENT_ID, queryMap.get("client_id"));
        assertEquals("http://localhost:4663", queryMap.get("redirect_uri"));
        assertEquals("code", queryMap.get("response_type"));
        assertEquals("select_account", queryMap.get("prompt"));
        assertEquals(AzureEnvironment.AZURE.activeDirectoryResourceId(), queryMap.get("resource"));

        url = AzureAuthHelper.authorizationUrl(AzureEnvironment.AZURE_CHINA, "http://localhost:4664");
        queryMap = splitQuery(url);
        assertEquals(Constants.CLIENT_ID, queryMap.get("client_id"));
        assertEquals("http://localhost:4664", queryMap.get("redirect_uri"));
        assertEquals("code", queryMap.get("response_type"));
        assertEquals("select_account", queryMap.get("prompt"));
        assertEquals(AzureEnvironment.AZURE_CHINA.activeDirectoryResourceId(), queryMap.get("resource"));
    }

    @Test
    public void tesAuthorizationUrlInvalidParameter() throws Exception {
        try {
            AzureAuthHelper.authorizationUrl(null, "http://localhost:4663");
            fail("Should throw IAE when env is null.");
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            AzureAuthHelper.authorizationUrl(AzureEnvironment.AZURE_CHINA, "");
            fail("Should throw IAE when redirectUrl is empty.");
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Test
    public void testBaseURL() {
        String baseUrl = AzureAuthHelper.baseURL(AzureEnvironment.AZURE);
        assertEquals("https://login.microsoftonline.com/common", baseUrl);
        baseUrl = AzureAuthHelper.baseURL(AzureEnvironment.AZURE_US_GOVERNMENT);
        assertEquals("https://login.microsoftonline.us/common", baseUrl);
    }

    private static Map<String, String> splitQuery(String url) throws UnsupportedEncodingException, MalformedURLException {
        final Map<String, String> queryMap = new LinkedHashMap<>();
        final List<NameValuePair> params = URLEncodedUtils.parse(new URL(url).getQuery(), Constants.UTF8);
        for (final NameValuePair param : params) {
            queryMap.put(param.getName(), param.getValue());
        }

        return queryMap;
    }

}
