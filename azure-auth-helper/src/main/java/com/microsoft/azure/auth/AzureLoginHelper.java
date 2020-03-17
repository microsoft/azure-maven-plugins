/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.AzureLoginTimeoutException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.common.utils.TextUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class AzureLoginHelper {
    private static final Map<AzureEnvironment, String> AZURE_ENVIRONMENT_MAP = new HashMap<>();

    static {
        final AzureEnvironment[] knownEnvironments = AzureEnvironment.knownEnvironments();

        for (final Field field : AzureEnvironment.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    final Object obj = FieldUtils.readStaticField(field);
                    if (ArrayUtils.contains(knownEnvironments, obj)) {
                        AZURE_ENVIRONMENT_MAP.put((AzureEnvironment) obj, field.getName().toLowerCase());
                    }
                } catch (IllegalAccessException e) {
                    // ignore
                }

            }
        }
    }

    /**
     * Performs an OAuth 2.0 login.
     *
     * @param env the azure environment
     * @return the azure credential
     * @throws DesktopNotSupportedException when the desktop is not supported
     * @throws AzureLoginFailureException when there are some errors during login.
     * @throws ExecutionException if there are some errors acquiring security token.
     * @throws InterruptedException if the current thread was interrupted.
     */
    static AzureCredential oAuthLogin(AzureEnvironment env)
            throws AzureLoginFailureException, ExecutionException, DesktopNotSupportedException, InterruptedException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new DesktopNotSupportedException("Not able to launch a browser to log you in.");
        }

        final LocalAuthServer server = new LocalAuthServer();
        try {
            server.start();
            final URI redirectUri = server.getURI();
            final String redirectUrl = redirectUri.toString();
            final String code;
            try {
                final String authorizationUrl = authorizationUrl(env, redirectUrl);
                Desktop.getDesktop().browse(new URL(authorizationUrl).toURI());
                code = server.waitForCode();
            } catch (InterruptedException e) {
                throw new AzureLoginFailureException("The OAuth flow is interrupted.");
            } finally {
                server.stop();
            }
            final AzureCredential cred = new AzureContextExecutor(baseURL(env), context -> context
                    .acquireTokenByAuthorizationCode(code, env.managementEndpoint(), Constants.CLIENT_ID, redirectUri, null).get()).execute();
            cred.setEnvironment(getShortNameForAzureEnvironment(env));
            return cred;
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
     * @throws ExecutionException if there are some errors acquiring security token.
     * @throws InterruptedException if the current thread was interrupted.
     * @throws MalformedURLException if there are some bad urls in azure endpoints
     */
    static AzureCredential deviceLogin(AzureEnvironment env)
            throws AzureLoginFailureException, MalformedURLException, InterruptedException, ExecutionException {
        final String currentLogLevelFieldName = "currentLogLevel";
        Object logger = null;
        Object oldLevelValue = null;

        try {
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
                System.out.println("Failed to disable the log of AuthenticationContext, it will continue being noisy.");
            }
            final AzureCredential cred = new AzureContextExecutor(baseURL(env), authenticationContext -> {
                final DeviceCode deviceCode = authenticationContext.acquireDeviceCode(Constants.CLIENT_ID, env.managementEndpoint(), null).get();
                // print device code hint message:
                // to sign in, use a web browser to open the page
                // https://microsoft.com/devicelogin and enter the code xxxxxx to authenticate.
                System.out.println(TextUtils.yellow(deviceCode.getMessage()));
                long remaining = deviceCode.getExpiresIn();
                final long interval = deviceCode.getInterval();
                while (remaining > 0) {
                    try {
                        remaining -= interval;
                        Thread.sleep(Duration.ofSeconds(interval).toMillis());
                        return authenticationContext.acquireTokenByDeviceCode(deviceCode, null).get();
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
                throw new AzureLoginTimeoutException(
                        String.format("Cannot proceed with device login after waiting for %d minutes.", deviceCode.getExpiresIn() / 60));
            }).execute();
            cred.setEnvironment(getShortNameForAzureEnvironment(env));
            return cred;
        } finally {
            try {
                if (logger != null) {
                    FieldUtils.writeField(logger, currentLogLevelFieldName, oldLevelValue, true);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // ignore
                System.out.println("Failed to reset the log level of AuthenticationContext.");
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
     * @throws ExecutionException if there are some errors acquiring security token.
     * @throws InterruptedException if the current thread was interrupted.
     * @throws MalformedURLException if there are some bad urls in azure endpoints
     */
    static AzureCredential refreshToken(AzureEnvironment env, String refreshToken)
            throws MalformedURLException, InterruptedException, ExecutionException {
        if (env == null) {
            throw new IllegalArgumentException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(refreshToken)) {
            throw new IllegalArgumentException("Parameter 'refreshToken' cannot be empty.");
        }

        try {
            return new AzureContextExecutor(baseURL(env), authenticationContext -> authenticationContext
                    .acquireTokenByRefreshToken(refreshToken, Constants.CLIENT_ID, env.managementEndpoint(), null).get()).execute();
        } catch (AzureLoginTimeoutException e) {
            // ignore: it will never throw during refreshing
            return null;
        }
    }

    /**
     * Convert an AzureEnvironment instance to the short name, eg: azure, azure_china, azure_germany, azure_us_government.
     *
     * @param env the AzureEnvironment instance
     * @return the short name
     */
    static String getShortNameForAzureEnvironment(AzureEnvironment env) {
        return AZURE_ENVIRONMENT_MAP.get(env);
    }

    static String authorizationUrl(AzureEnvironment env, String redirectUrl) throws URISyntaxException, MalformedURLException {
        if (env == null) {
            throw new IllegalArgumentException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(redirectUrl)) {
            throw new IllegalArgumentException("Parameter 'redirectUrl' cannot be empty.");
        }

        final URIBuilder builder = new URIBuilder(baseURL(env));
        builder.setPath(String.format("%s/oauth2/authorize", builder.getPath()))
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

    private AzureLoginHelper() {

    }
}
