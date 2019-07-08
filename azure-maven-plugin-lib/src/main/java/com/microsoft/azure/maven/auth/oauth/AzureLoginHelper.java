/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth.oauth;

import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.maven.auth.AzureCredential;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureLoginHelper {
    public static final String CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";
    private static final String AUTH_WITH_OAUTH = "Authenticate with OAuth 2.0";
    private static final String AUTH_WITH_DEVICE_LOGIN = "Authenticate with Device Login";        
    private static final String DOMAIN = "common";
    private static final String LOGIN_MESSAGE_TEMPLATE = 
            "To sign in, use a web browser to open the page %s and enter the code %s to authenticate.";
    
    public static String baseURL(AzureEnvironment env) {
        return env.activeDirectoryEndpoint() + DOMAIN;
    }
    
    public static AzureCredential oauthLogin(AzureEnvironment env) throws IOException {
        final WebAppServer webAppServer = new WebAppServer();
        final int port = webAppServer.getPort();
        final String baseURL = baseURL(env); 
        final String redirectUrl = "http://localhost:" + port;
        final String authorizationUrl = String.format(
                "%s/oauth2/authorize?client_id=%s&response_type=code" +
                "&redirect_uri=%s&&prompt=select_account&resource=%s",
                baseURL, CLIENT_ID, redirectUrl, env.managementEndpoint());
        try { 
            Desktop.getDesktop().browse(new URL(authorizationUrl).toURI());
            System.out.println(AUTH_WITH_OAUTH);
        } catch (Exception ex) {
            // the browse is not available
            
        }
        ExecutorService executorService = null;
        try {            
            webAppServer.start();
            executorService = Executors.newSingleThreadExecutor();
            final AuthenticationContext authenticationContext = new AuthenticationContext(baseURL, true, 
                    executorService);
            
            final AuthenticationResult result = authenticationContext.acquireTokenByAuthorizationCode(
                    webAppServer.getResult(), env.managementEndpoint(), CLIENT_ID,
                    new URI(redirectUrl), null).get();
            
            return new AzureCredential(env, result);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            webAppServer.stop();
            executorService.shutdown();
        }
    }
    
    public static AzureCredential deviceLogin(AzureEnvironment env) throws IOException {
        ExecutorService executorService = null;
        try {
            System.out.println(AUTH_WITH_DEVICE_LOGIN);
            executorService = Executors.newSingleThreadExecutor();
            final AuthenticationContext authenticationContext = 
                    new AuthenticationContext(baseURL(env), true, executorService);
            final DeviceCode deviceCode = authenticationContext.acquireDeviceCode(CLIENT_ID, 
                    env.activeDirectoryResourceId(), null).get();
            
            System.out.println(String.format(LOGIN_MESSAGE_TEMPLATE,
                    deviceCode.getVerificationUrl(), deviceCode.getUserCode()));
            try {
                // disable log4j of AuthenticationContext, otherwise the pending user authorization log
                // will be print every second.
                // see https://github.com/AzureAD/azure-activedirectory-library-for-java/issues/246
                final Object logger = LoggerFactory.getLogger(AuthenticationContext.class);  
                final Field levelField = logger.getClass().getSuperclass().getDeclaredField("currentLogLevel");
                levelField.setAccessible(true);
                levelField.set(logger, LocationAwareLogger.ERROR_INT + 1); 
            } catch (Exception e) {
                System.out.println("Failed to reset the log level of " +
                        AuthenticationContext.class.getName() + ", it will continue being noisy.");
            }
            long remaining = deviceCode.getExpiresIn();
            final long interval = deviceCode.getInterval();
            AuthenticationResult authenticationResult = null;
            while (remaining > 0 && authenticationResult == null) {
                try {
                    remaining -= interval;
                    Thread.sleep(interval * 1000);
                    authenticationResult = authenticationContext.acquireTokenByDeviceCode(deviceCode, null).get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof AuthenticationException &&
                            ((AuthenticationException) e.getCause()).getErrorCode() == 
                                AdalErrorCode.AUTHORIZATION_PENDING) {
                        // swallow the pending exception
                    } else {
                        System.out.println(e.getMessage());
                        break;
                    }
                }
            }
            return new AzureCredential(env, authenticationResult);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            executorService.shutdown();
        }
    }
}
