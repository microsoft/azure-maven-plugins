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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

public class AzureAuthHelper {
    private static final String AUTH_WITH_OAUTH = "Authenticate with OAuth";
    private static final String AUTH_WITH_DEVICE_LOGIN = "Authenticate with Device Login";

    /**
     * Performs an OAuth 2.0 login.
     *
     * @param env the azure environment
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during login.
     */
    public static AzureCredential oAuthLogin(AzureEnvironment env) throws AzureLoginFailureException {

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return null;
        }

        final LocalAuthServer server = new LocalAuthServer();
        try {
            server.start();
            final URI redirectUri = server.getURI();
            final String redirectUrl = redirectUri.toString();

            try {
                final String authorizationUrl = authorizationUrl(env, redirectUrl);
                Desktop.getDesktop().browse(new URL(authorizationUrl).toURI());
                System.out.println(AUTH_WITH_OAUTH);
                final String code = server.waitForCode();
                return new AzureContextExecutor(baseURL(env), context -> context.acquireTokenByAuthorizationCode(code,
                        env.managementEndpoint(), Constants.CLIENT_ID, redirectUri, null).get()).execute();
            } catch (InterruptedException e) {
                throw new AzureLoginFailureException("The OAuth flow is interrupted.");
            } finally {
                server.stop();
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
        final String currentLogLevelFieldName = "currentLogLevel";
        Object logger = null;
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
                if (logger != null) {
                    oldLevelValue = FieldUtils.readField(logger, currentLogLevelFieldName, true);
                    FieldUtils.writeField(logger, currentLogLevelFieldName, LocationAwareLogger.ERROR_INT + 1, true);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                System.out.println("Failed to disable the log of " + AuthenticationContext.class.getName() + ", it will continue being noisy.");
            }
            return new AzureContextExecutor(baseURL(env), authenticationContext -> {
                final DeviceCode deviceCode = authenticationContext.acquireDeviceCode(Constants.CLIENT_ID, env.activeDirectoryResourceId(), null).get();
                // print device code hint message:
                // to sign in, use a web browser to open the page
                // https://microsoft.com/devicelogin and enter the code xxxxxx to authenticate.
                // TODO: add a color wrap
                System.err.println(deviceCode.getMessage());
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
                if (result == null) {
                    throw new AzureLoginFailureException("Cannot proceed with device login after waiting for " + deviceCode.getExpiresIn() / 60 + " minutes.");
                }
                return result;
            }).execute();
        } finally {
            try {
                FieldUtils.writeField(logger, currentLogLevelFieldName, oldLevelValue, true);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // ignore
                System.out.println("Failed to reset the log level of " + AuthenticationContext.class.getName());
            }
        }

    }

    /**
     * Refresh an azure credential using refresh token.
     *
     * @param env the azure environment
     * @param refreshToken the refresh token
     *
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during refreshing.
     */
    public static AzureCredential refreshToken(AzureEnvironment env, String refreshToken) throws AzureLoginFailureException {
        if (env == null) {
            throw new IllegalArgumentException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(refreshToken)) {
            throw new IllegalArgumentException("Parameter 'refreshToken' cannot be empty.");
        }

        return new AzureContextExecutor(baseURL(env), authenticationContext -> authenticationContext
                .acquireTokenByRefreshToken(refreshToken, Constants.CLIENT_ID, env.managementEndpoint(), null).get()).execute();
    }

    /**
     * Get the azure-secret.json file according to environment variable, the default location is $HOME/.azure/azure-secret.json
     */
    public static File getAzureSecretFile() {
        return (StringUtils.isBlank(System.getProperty(Constants.AZURE_HOME_KEY)) ?
                Paths.get(System.getProperty(Constants.USER_HOME_KEY), Constants.AZURE_HOME_DEFAULT, Constants.AZURE_SECRET_FILE)
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

    static String authorizationUrl(AzureEnvironment env, String redirectUrl) throws URISyntaxException, MalformedURLException {
        if (env == null) {
            throw new IllegalArgumentException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(redirectUrl)) {
            throw new IllegalArgumentException("Parameter 'redirectUrl' cannot be empty.");
        }

        final URIBuilder builder = new URIBuilder(baseURL(env));
        builder.setPath(builder.getPath() + "/oauth2/authorize")
            .setParameter("client_id", Constants.CLIENT_ID)
            .setParameter("response_type", "code")
            .setParameter("redirect_uri", redirectUrl)
            .setParameter("prompt", "select_account")
            .setParameter("resource", env.managementEndpoint());
        return builder.build().toURL().toString();
    }

    static String baseURL(AzureEnvironment env) {
        return env.activeDirectoryEndpoint() + Constants.COMMON_TENANT;
    }

    private AzureAuthHelper() {

    }
}
