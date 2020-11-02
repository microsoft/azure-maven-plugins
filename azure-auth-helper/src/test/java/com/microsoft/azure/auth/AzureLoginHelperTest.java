/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.exception.DesktopNotSupportedException;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalAuthServer.class, Desktop.class, AzureLoginHelper.class,
        AuthenticationContext.class, Future.class, AzureContextExecutor.class, DeviceCode.class})
public class AzureLoginHelperTest {
    @Test
    public void testOauthWithBrowser() throws Exception {
        PowerMockito.mockStatic(Desktop.class);
        final URI uri = new URI("http://0.0.0.0");
        final Desktop mockDesktop = mock(Desktop.class);
        PowerMockito.doNothing().when(mockDesktop).browse(uri);
        PowerMockito.when(mockDesktop.isSupported(Desktop.Action.BROWSE)).thenReturn(true);
        PowerMockito.when(Desktop.isDesktopSupported()).thenReturn(true);
        PowerMockito.when(Desktop.getDesktop()).thenReturn(mockDesktop);

        mockStatic(LocalAuthServer.class);
        final LocalAuthServer mockServer = mock(LocalAuthServer.class);
        whenNew(LocalAuthServer.class).withNoArguments().thenReturn(mockServer);
        PowerMockito.doNothing().when(mockServer).start();
        when(mockServer.getURI()).thenReturn(uri);
        when(mockServer.waitForCode()).thenReturn("test code");

        PowerMockito.doNothing().when(mockServer).stop();
        final AuthenticationResult authenticationResult = TestHelper.createAuthenticationResult();
        final AuthenticationContext ctx = mock(AuthenticationContext.class);
        final AzureEnvironment env = AzureEnvironment.AZURE;
        final Future future = mock(Future.class);
        whenNew(AuthenticationContext.class).withAnyArguments().thenReturn(ctx);
        when(future.get()).thenReturn(authenticationResult);
        when(ctx.acquireTokenByAuthorizationCode("test code", env.managementEndpoint(), Constants.CLIENT_ID,
                uri, null)).thenReturn(future);

        final AzureCredential credFromOAuth = AzureLoginHelper.oAuthLogin(AzureEnvironment.AZURE);
        assertEquals("azure", credFromOAuth.getEnvironment());
        assertEquals(authenticationResult.getAccessToken(), credFromOAuth.getAccessToken());
        assertEquals(authenticationResult.getRefreshToken(), credFromOAuth.getRefreshToken());
    }

    @Test
    public void testOAuthLoginNoBrowser() throws Exception {
        PowerMockito.mockStatic(Desktop.class);
        PowerMockito.when(Desktop.isDesktopSupported()).thenReturn(false);
        // we cannot test oauth when desktop is not supported
        try {
            AzureLoginHelper.oAuthLogin(AzureEnvironment.AZURE);
            fail("Should report desktop not supported.");
        } catch (DesktopNotSupportedException e) {
            // expected
        }
    }

    @Test
    public void testDeviceLogin() throws Exception {
        final AuthenticationResult authenticationResult = TestHelper.createAuthenticationResult();
        final AuthenticationContext ctx = mock(AuthenticationContext.class);
        final AzureEnvironment env = AzureEnvironment.AZURE;
        final Future future = mock(Future.class);
        final DeviceCode deviceCode = mock(DeviceCode.class);
        when(deviceCode.getMessage()).thenReturn("Mock message");
        when(deviceCode.getExpiresIn()).thenReturn(Long.valueOf(3600));
        when(deviceCode.getInterval()).thenReturn(Long.valueOf(1));

        whenNew(AuthenticationContext.class).withAnyArguments().thenReturn(ctx);
        when(future.get()).thenReturn(deviceCode);
        when(ctx.acquireDeviceCode(Constants.CLIENT_ID, env.managementEndpoint(), null))
            .thenReturn(future);
        final Future future2 = mock(Future.class);
        when(future2.get()).thenReturn(authenticationResult);
        when(ctx.acquireTokenByDeviceCode(deviceCode, null)).thenReturn(future2);
        final AzureCredential credFromDeviceLogin = AzureLoginHelper.deviceLogin(AzureEnvironment.AZURE);
        assertEquals("azure", credFromDeviceLogin.getEnvironment());
        assertEquals(authenticationResult.getAccessToken(), credFromDeviceLogin.getAccessToken());
        assertEquals(authenticationResult.getRefreshToken(), credFromDeviceLogin.getRefreshToken());
    }

    @Test
    public void testRefreshToken() throws Exception {
        final AuthenticationResult authenticationResult = TestHelper.createAuthenticationResult();
        final AuthenticationContext ctx = mock(AuthenticationContext.class);
        final AzureEnvironment env = AzureEnvironment.AZURE;
        final Future future = mock(Future.class);
        whenNew(AuthenticationContext.class).withAnyArguments().thenReturn(ctx);
        when(future.get()).thenReturn(authenticationResult);
        when(ctx.acquireTokenByRefreshToken("token for power mock", Constants.CLIENT_ID, env.managementEndpoint(), null))
            .thenReturn(future);
        final Map<String, Object> map = TestHelper.getAuthenticationMap();
        final AzureCredential cred = AzureLoginHelper.refreshToken(env, "token for power mock");
        assertNotNull(cred);
        assertEquals(map.get("accessTokenType"), cred.getAccessTokenType());
        assertEquals(map.get("accessToken"), cred.getAccessToken());
        assertEquals(map.get("refreshToken"), cred.getRefreshToken());
        assertEquals(map.get("idToken"), cred.getIdToken());
        assertEquals(map.get("isMultipleResourceRefreshToken"), cred.isMultipleResourceRefreshToken());
    }

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

    @Test
    public void testAzureEnvironmentShortName() {
        assertEquals("azure", AzureLoginHelper.getShortNameForAzureEnvironment(AzureEnvironment.AZURE));
        assertEquals("azure_china", AzureLoginHelper.getShortNameForAzureEnvironment(AzureEnvironment.AZURE_CHINA));
        assertEquals("azure_us_government", AzureLoginHelper.getShortNameForAzureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT));
        assertNull(AzureLoginHelper.getShortNameForAzureEnvironment(null));
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

