/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azure.AzureEnvironment;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

public class AzureAuthHelper {
    private static final String AUTH_WITH_OAUTH = "Authenticate with OAuth 2.0";
    private static final String AUTH_WITH_DEVICE_LOGIN = "Authenticate with Device Login";
    private static final String DEVICE_LOGIN_MESSAGE_TEMPLATE = "To sign in, use a web browser to open the page %s and enter the code %s to authenticate.";

    /**
     * Performs an OAuth 2.0 login.
     *
     * @param env the azure environment
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during login.
     */
    public static AzureCredential oAuthLogin(AzureEnvironment env) throws AzureLoginFailureException {
        try {
            final WebAppServer webAppServer = new WebAppServer();
            final String redirectUrl = webAppServer.getUrl();
            final URI redirectUri = new URI(redirectUrl);
            try {
                final String authorizationUrl = authorizationUrl(env, redirectUrl);
                try {
                    Desktop.getDesktop().browse(new URL(authorizationUrl).toURI());
                    System.out.println(AUTH_WITH_OAUTH);
                } catch (Exception ex) {
                    // the browse is not available
                    return null;
                }

                webAppServer.start();
                return new AzureCredentialCallable(baseURL(env), context -> context.acquireTokenByAuthorizationCode(webAppServer.getResult(),
                        env.managementEndpoint(), Constants.CLIENT_ID, redirectUri, null).get()
                ).call();
            } finally {
                if (webAppServer != null) {
                    webAppServer.stop();
                }

            }

        } catch (IOException | URISyntaxException e) {
            throw new AzureLoginFailureException(e.getMessage());
        }
    }

    /**
     * Performs a device login.
     *
     * @param env the azure environment
     * @return the azure credential through
     * @throws AzureLoginFailureException when there are some errors during login.
     */
    public static AzureCredential deviceLogin(AzureEnvironment env) throws AzureLoginFailureException {
        Object logger = null;
        Field levelField = null;
        Object oldLevelValue = null;

        try {
            System.out.println(AUTH_WITH_DEVICE_LOGIN);

            try {
                // disable log4j of AuthenticationContext, otherwise the pending user
                // authorization log
                // will be print every second.
                // see
                // https://github.com/AzureAD/azure-activedirectory-library-for-java/issues/246
                logger = LoggerFactory.getLogger(AuthenticationContext.class);
                levelField = logger.getClass().getSuperclass().getDeclaredField("currentLogLevel");
                oldLevelValue = setObjectValue(logger, levelField, LocationAwareLogger.ERROR_INT + 1);
            } catch (Exception e) {
                System.out.println("Failed to disable the log of " + AuthenticationContext.class.getName() + ", it will continue being noisy.");
            }
            return new AzureCredentialCallable(baseURL(env), authenticationContext -> {
                final DeviceCode deviceCode = authenticationContext.acquireDeviceCode(Constants.CLIENT_ID, env.activeDirectoryResourceId(), null).get();
                System.out.println(String.format(DEVICE_LOGIN_MESSAGE_TEMPLATE, deviceCode.getVerificationUrl(), deviceCode.getUserCode()));
                long remaining = deviceCode.getExpiresIn();
                final long interval = deviceCode.getInterval();
                AuthenticationResult result = null;
                while (remaining > 0 && result == null) {
                    try {
                        remaining -= interval;
                        Thread.sleep(interval * 1000);
                        result = authenticationContext.acquireTokenByDeviceCode(deviceCode, null).get();
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof AuthenticationException &&
                                ((AuthenticationException) e.getCause()).getErrorCode() == AdalErrorCode.AUTHORIZATION_PENDING) {
                            // swallow the pending exception
                        } else {
                            // TODO: need to add a logger to the parameter
                            System.out.println(e.getMessage());
                            break;
                        }
                    }
                }
                return result;
            }).call();
        } finally {
            try {
                // reset currentLogLevel to the logger in AuthenticationContext
                setObjectValue(logger, levelField, oldLevelValue);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // ignore
                System.out.println("Failed to reset the log level of " + AuthenticationContext.class.getName());
            }
        }

    }

    /**
     * Refresh an azure credential using refresh token.
     *
     * @param env          the azure environment
     * @param refreshToken the refresh token
     *
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during
     *                                    refreshing.
     */
    public static AzureCredential refreshToken(AzureEnvironment env, String refreshToken)
            throws AzureLoginFailureException {
        if (env == null) {
            throw new NullPointerException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(refreshToken)) {
            throw new IllegalArgumentException("Parameter 'refreshToken' cannot be empty.");
        }

        return new AzureCredentialCallable(baseURL(env), authenticationContext -> authenticationContext
                .acquireTokenByRefreshToken(refreshToken, Constants.CLIENT_ID, env.managementEndpoint(), null).get())
                        .call();
    }

    /**
     * Get the azure-secret.json file according to environment variable, the default
     * location is $HOME/.azure/azure-secret.json
     */
    public static File getAzureSecretFile() {
        return (StringUtils.isBlank(System.getProperty(Constants.AZURE_HOME_KEY)) ?
                Paths.get(System.getProperty(Constants.USER_HOME_KEY), Constants.AZURE_HOME_DEFAULT,
                        Constants.AZURE_SECRET_FILE)
                : Paths.get(System.getProperty(Constants.AZURE_HOME_KEY), Constants.AZURE_SECRET_FILE)).toFile();
    }

    /***
     * Save the credential to a file.
     *
     * @param cred the credential
     * @param file the file name to save the credential
     * @throws IOException if there is any IO error.
     */
    public static void writeAzureCredentials(AzureCredential cred, File file) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /***
     * Read the credential from a file.
     *
     * @param file the file to be read
     * @return the saved credential
     * @throws IOException if there is any IO error.
     */
    public static AzureCredential readAzureCredentials(File file) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    static String authorizationUrl(AzureEnvironment env, String redirectUrl) {
        if (env == null) {
            throw new NullPointerException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(redirectUrl)) {
            throw new IllegalArgumentException("Parameter 'redirectUrl' cannot be empty.");
        }
        return String.format(
                "%s/oauth2/authorize?client_id=%s&response_type=code" +
                        "&redirect_uri=%s&prompt=select_account&resource=%s",
                baseURL(env), Constants.CLIENT_ID, redirectUrl, env.managementEndpoint());
    }

    static String baseURL(AzureEnvironment env) {
        return env.activeDirectoryEndpoint() + Constants.COMMON_TENANT;
    }

    private static Object setObjectValue(Object obj, Field field, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        final Object oldLevelValue = field.get(obj);
        if (value == null) {
            field.set(obj, value);
        }

        return oldLevelValue;
    }

    private AzureAuthHelper() {

    }
}
