/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationResult;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AzureCredentialTest {
    @Test
    public void testConstructor() {
        final String authJson = "{\n" +
                "    \"accessTokenType\": \"Bearer\",\n" +
                "    \"idToken\": \"eyJ0eXAi...iOiIxLjAifQ.\",\n" +
                "    \"userInfo\": {\n" +
                "        \"uniqueId\": \"daaaa...3f2\",\n" +
                "        \"displayableId\": \"george@microsoft.com\",\n" +
                "        \"givenName\": \"George\",\n" +
                "        \"familyName\": \"Smith\",\n" +
                "        \"tenantId\": \"72f988bf-86f1-41af-91ab-2d7cd011db47\"\n" +
                "    },\n" +
                "    \"accessToken\": \"eyJ0eXA...jmcnxMnQ\",\n" +
                "    \"refreshToken\": \"AQAB...n5cgAA\",\n" +
                "    \"isMultipleResourceRefreshToken\": false\n" +
                "}";
        final AuthenticationResult result = JsonUtils.fromJson(authJson, AuthenticationResult.class);
        final AzureCredential cred = AzureCredential.fromAuthenticationResult(result);
        final Map<String, Object> map = JsonUtils.fromJson(authJson, Map.class);
        assertNotNull(cred);
        assertEquals(map.get("accessTokenType"), cred.getAccessTokenType());
        assertEquals(map.get("accessToken"), cred.getAccessToken());
        assertEquals(map.get("refreshToken"), cred.getRefreshToken());
        assertEquals(map.get("idToken"), cred.getIdToken());
        assertEquals(map.get("isMultipleResourceRefreshToken"), cred.isMultipleResourceRefreshToken());

        assertEquals("daaaa...3f2", cred.getUserInfo().getUniqueId());
        assertEquals("george@microsoft.com", cred.getUserInfo().getDisplayableId());
        assertEquals("George", cred.getUserInfo().getGivenName());
        assertEquals("Smith", cred.getUserInfo().getFamilyName());
    }

    @Test(expected = NullPointerException.class)
    public void testFromNullResult() {
        AzureCredential.fromAuthenticationResult(null);
        fail("Should throw NPE");
    }
}
