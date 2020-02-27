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
        final AuthenticationResult result = TestHelper.createAuthenticationResult();
        final Map<String, Object> map = TestHelper.getAuthenticationMap();
        final AzureCredential cred = AzureCredential.fromAuthenticationResult(result);
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

        cred.setEnvironment("azure");
        cred.setDefaultSubscription("c58a8b4f-00c8-436d-895c-6d5003e87e4e");
        assertEquals("azure", cred.getEnvironment());
        assertEquals("c58a8b4f-00c8-436d-895c-6d5003e87e4e", cred.getDefaultSubscription());
    }

    @Test
    public void testFromNullResult() {
        try {
            AzureCredential.fromAuthenticationResult(null);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // IllegalArgumentException expected
        }
    }
}
