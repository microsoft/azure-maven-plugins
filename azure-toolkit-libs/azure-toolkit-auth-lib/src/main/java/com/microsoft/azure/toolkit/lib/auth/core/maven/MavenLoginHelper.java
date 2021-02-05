/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.maven;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import reactor.core.publisher.Mono;
import rx.exceptions.Exceptions;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class MavenLoginHelper {
    // ClientId from https://github.com/Azure/azure-cli/blob/1beb6352ece2d06187bbccd66f1638f45b0340f7/src/azure-cli-core/azure/cli/core/_profile.py#L64
    private static final String CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";
    private static final String COMMON_TENANT = "common";
    private static final String AZURE_FOLDER = ".azure";
    private static final String USER_HOME = "user.home";
    private static final String AZURE_CONFIG_DIR = "AZURE_CONFIG_DIR";
    private static final String AZURE_SECRET_FILE = "azure-secret.json";
    private static final String AZURE_PROFILE_NAME = "azureProfile.json";
    private static final String AZURE_TOKEN_NAME = "accessTokens.json";

    /**
     * Get the azure config folder location, the default location is $HOME/.azure.
     */
    public static File getAzureConfigFolder() {
        return StringUtils.isNotBlank(System.getenv(AZURE_CONFIG_DIR)) ? new File(System.getenv(AZURE_CONFIG_DIR)) :
                Paths.get(System.getProperty(USER_HOME), AZURE_FOLDER).toFile();
    }

    /**
     * Get the azure-secret.json file according to environment variable, the default location is $HOME/.azure/azure-secret.json
     */
    public static File getAzureSecretFile() {
        return new File(getAzureConfigFolder(), AZURE_SECRET_FILE);
    }

    /**
     * Check whether the azure-secret.json file exists and is not empty.
     */
    public static boolean existsAzureSecretFile() {
        final File azureSecretFile = getAzureSecretFile();
        return azureSecretFile.exists() && azureSecretFile.isFile() && azureSecretFile.length() > 0;
    }

    /***
     * Read the credential from a file.
     *
     * @param file the file to be read
     * @return the saved credential
     * @throws IOException if there is any IO error.
     */
    public static AzureCredential readAzureCredentials(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Parameter 'file' cannot be null.");
        }
        final String jsonStr = FileUtils.readFileToString(file, "utf8");
        return JsonUtils.fromJson(jsonStr, AzureCredential.class);
    }

    /**
     * Get azure track 2 credential from AzureCredential instance.
     *
     * @param credentials the azure credential
     * @param env         the azure environment
     * @return the azure token credential can be used in Azure SDK.
     */
    public static TokenCredential getMavenAzureLoginCredentialsTrack2(@Nonnull AzureCredential credentials, @Nonnull AzureEnvironment env) throws IOException {
        return new TokenCredential() {
            private Map<String, AzureTokenCredentials> tokenCache = new ConcurrentHashMap<>();
            @Override
            public Mono<AccessToken> getToken(TokenRequestContext request) {
                final String resource = ScopeUtil.scopesToResource(request.getScopes());
                final AzureTokenCredentials track1Token = tokenCache.computeIfAbsent(resource, (ignore) -> {
                    return getMavenAzureLoginCredentialsTrack1(credentials, env);
                });
                return Mono.fromCallable(() -> {
                    try {
                        // token expiration is handle in getMavenAzureLoginCredentialsTrack1
                        return new AccessToken(track1Token.getToken(resource), OffsetDateTime.MAX);
                    } catch (IOException e) {
                        Exceptions.propagate(e);

                    }
                    return null;
                });
            }
        };
    }

    /**
     * Get azure track l credential from AzureCredential instance.
     *
     * @param credentials the azure credential
     * @param env         the azure environment
     * @return the azure token credential can be used in Azure SDK.
     */
    public static AzureTokenCredentials getMavenAzureLoginCredentialsTrack1(AzureCredential credentials, AzureEnvironment env) {
        final AzureTokenCredentials azureTokenCredentials = new AzureTokenCredentials(env, null) {
            @Override
            public String getToken(String resource) throws IOException {
                final String accessToken = credentials.getAccessToken();
                final String accessTokenWithoutSignature = accessToken.substring(0, accessToken.lastIndexOf('.') + 1);
                try {
                    final Jwt<?, Claims> jwtToken = Jwts.parser().parseClaimsJwt(accessTokenWithoutSignature);
                    // add 1 minute to avoid the edge case of expired token right after checking
                    if (jwtToken.getBody().getExpiration().after(DateUtils.addMinutes(new Date(), 1))) {
                        return accessToken;
                    }
                } catch (ExpiredJwtException ex) {
                    // ignore
                }
                try {
                    final AzureCredential newCredentials = refreshToken(env, credentials.getRefreshToken());
                    credentials.setAccessToken(newCredentials.getAccessToken());
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof AuthenticationException) {
                        throw (AuthenticationException) e.getCause();
                    }
                    if (e.getCause() instanceof IOException) {
                        throw (IOException) e.getCause();
                    }
                    // because get token method declares throwing IOException
                    throw new IOException(String.format("Error happened during refreshing access token, due to error: %s.", e.getMessage()));
                }

                return credentials.getAccessToken();
            }
        };
        if (StringUtils.isNotBlank(credentials.getDefaultSubscription())) {
            azureTokenCredentials.withDefaultSubscriptionId(credentials.getDefaultSubscription());
        }
        return azureTokenCredentials;
    }

    /**
     * Refresh an azure credential using refresh token.
     *
     * @param env          the azure environment
     * @param refreshToken the refresh token
     * @return the azure credential
     * @throws ExecutionException    if there are some errors acquiring security token.
     * @throws InterruptedException  if the current thread was interrupted.
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

        return new AzureContextExecutor(baseURL(env), authenticationContext -> authenticationContext
                .acquireTokenByRefreshToken(refreshToken, CLIENT_ID, env.managementEndpoint(), null).get()).execute();

    }

    static String baseURL(AzureEnvironment env) {
        return env.activeDirectoryEndpoint() + COMMON_TENANT;
    }
}
