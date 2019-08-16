/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.maven.utils.JsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, AzureContextExecutor.class,
    Future.class, AzureLoginHelper.class, AzureAuthHelper.class,
    AuthenticationContext.class, AuthenticationContext.class})
public class AzureAuthHelperTest {
    @Test
    public void testRefreshToken() throws Exception {
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
        final AuthenticationResult authenticationResult = JsonUtils.fromJson(authJson, AuthenticationResult.class);
        final AuthenticationContext ctx = mock(AuthenticationContext.class);
        final AzureEnvironment env = AzureEnvironment.AZURE;
        final Future future = mock(Future.class);
        whenNew(AuthenticationContext.class).withAnyArguments().thenReturn(ctx);
        when(future.get()).thenReturn(authenticationResult);
        when(ctx.acquireTokenByRefreshToken("token for power mock", Constants.CLIENT_ID, env.managementEndpoint(), null))
            .thenReturn(future);
        final Map<String, Object> map = JsonUtils.fromJson(authJson, Map.class);
        final AzureCredential cred = AzureAuthHelper.refreshToken(env, "token for power mock");
        assertEquals(map.get("accessTokenType"), cred.getAccessTokenType());
        assertEquals(map.get("accessToken"), cred.getAccessToken());
        assertEquals(map.get("refreshToken"), cred.getRefreshToken());
        assertEquals(map.get("idToken"), cred.getIdToken());
        assertEquals(map.get("isMultipleResourceRefreshToken"), cred.isMultipleResourceRefreshToken());

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
        mockStatic(System.class);
        when(System.getenv("AZURE_CONFIG_DIR")).thenReturn("test_dir");
        assertEquals(Paths.get("test_dir", "azure-secret.json").toFile().getAbsolutePath(), AzureAuthHelper.getAzureSecretFile().getAbsolutePath());
    }

    @Test
    public void testWriteAzureCredentials() throws Exception {
    }

    @Test
    public void testReadAzureCredentials() throws Exception {
    }
}

