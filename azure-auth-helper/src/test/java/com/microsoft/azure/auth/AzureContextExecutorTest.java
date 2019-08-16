/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.maven.utils.JsonUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class AzureContextExecutorTest {
    @Test
    public void testCtorBadParamters() {
        try {
            new AzureContextExecutor(null, t -> null);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // IllegalArgumentException expected
        }

        try {
            new AzureContextExecutor("", t -> null);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // IllegalArgumentException expected
        }

        try {
            new AzureContextExecutor(AzureLoginHelper.baseURL(AzureEnvironment.AZURE), null);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // IllegalArgumentException expected
        }

        new AzureContextExecutor(AzureLoginHelper.baseURL(AzureEnvironment.AZURE), t -> null);
    }

    @Test
    public void testExecute() throws Exception {
        AzureContextExecutor executor = new AzureContextExecutor(AzureLoginHelper.baseURL(AzureEnvironment.AZURE), t -> null);
        assertNull(executor.execute());

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
                "    \"isMultipleResourceRefreshToken\": true\n" +
                "}";
        final AuthenticationResult result = JsonUtils.fromJson(authJson, AuthenticationResult.class);
        executor = new AzureContextExecutor(AzureLoginHelper.baseURL(AzureEnvironment.AZURE), t -> result);
        final AzureCredential cred = executor.execute();

        assertNotNull(cred);
        assertNotNull(cred.getAccessToken());
        assertNotNull(cred.getRefreshToken());
        assertNull(cred.getEnvironment());
        assertNull(cred.getDefaultSubscription());

    }
}
