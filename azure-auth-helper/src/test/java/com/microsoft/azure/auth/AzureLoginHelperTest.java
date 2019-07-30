/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AzureLoginHelperTest {
    @Test
    public void testAuthorizationUrl() throws Exception {
        String url = AzureLoginHelper.authorizationUrl(AzureEnvironment.AZURE, "http://localhost:4663");
        Map<String, String> queryMap = splitQuery(url);
        assertEquals(Constants.CLIENT_ID, queryMap.get("client_id"));
        assertEquals("http://localhost:4663", queryMap.get("redirect_uri"));
        assertEquals("code", queryMap.get("response_type"));
        assertEquals("select_account", queryMap.get("prompt"));
        assertEquals(AzureEnvironment.AZURE.activeDirectoryResourceId(), queryMap.get("resource"));

        url = AzureLoginHelper.authorizationUrl(AzureEnvironment.AZURE_CHINA, "http://localhost:4664");
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
            AzureLoginHelper.authorizationUrl(null, "http://localhost:4663");
            fail("Should throw IAE when env is null.");
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            AzureLoginHelper.authorizationUrl(AzureEnvironment.AZURE_CHINA, "");
            fail("Should throw IAE when redirectUrl is empty.");
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Test
    public void testBaseURL() {
        String baseUrl = AzureLoginHelper.baseURL(AzureEnvironment.AZURE);
        assertEquals("https://login.microsoftonline.com/common", baseUrl);
        baseUrl = AzureLoginHelper.baseURL(AzureEnvironment.AZURE_US_GOVERNMENT);
        assertEquals("https://login.microsoftonline.us/common", baseUrl);
    }

    private static Map<String, String> splitQuery(String url) throws URISyntaxException {
        final Map<String, String> queryMap = new LinkedHashMap<>();
        final List<NameValuePair> params = new URIBuilder(url).getQueryParams();
        for (final NameValuePair param : params) {
            queryMap.put(param.getName(), param.getValue());
        }

        return queryMap;
    }

}
