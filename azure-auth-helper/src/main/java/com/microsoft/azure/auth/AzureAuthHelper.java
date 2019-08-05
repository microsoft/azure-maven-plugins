/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.maven.utils.JsonUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class AzureAuthHelper {
    /**
     * Performs an OAuth 2.0 login.
     *
     * @param env the azure environment
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during login.
     */
    public static AzureCredential oAuthLogin(AzureEnvironment env)
            throws AzureLoginFailureException, DesktopNotSupportedException, InterruptedException, ExecutionException {
        return AzureLoginHelper.oAuthLogin(env);
    }

    /**
     * Performs a device login.
     *
     * @param env the azure environment
     * @return the azure credential through device code
     * @throws AzureLoginFailureException when there are some errors during login.
     * @throws ExecutionException if there are some errors acquiring security token.
     * @throws InterruptedException if the current thread was interrupted.
     * @throws MalformedURLException if there are some bad urls in azure endpoints
     */
    public static AzureCredential deviceLogin(AzureEnvironment env)
            throws AzureLoginFailureException, MalformedURLException, InterruptedException, ExecutionException {
        return AzureLoginHelper.deviceLogin(env);
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
    public static AzureCredential refreshToken(AzureEnvironment env, String refreshToken)
            throws MalformedURLException, InterruptedException, ExecutionException {
        return AzureLoginHelper.refreshToken(env, refreshToken);
    }

    /**
     * Get the corresponding azure environment
     * .
     * @param environment the environment key
     * @return the AzureEnvironment instance
     */
    public static AzureEnvironment getAzureEnvironment(String environment) {
        if (StringUtils.isEmpty(environment)) {
            return AzureEnvironment.AZURE;
        }

        switch (environment.toUpperCase(Locale.ENGLISH)) {
            case "AZURE_CHINA":
            case "AZURECHINACLOUD": // this value comes from azure cli
                return AzureEnvironment.AZURE_CHINA;
            case "AZURE_GERMANY":
            case "AZUREGERMANCLOUD": // this value comes from azure cli
                return AzureEnvironment.AZURE_GERMANY;
            case "AZURE_US_GOVERNMENT":
            case "AZUREUSGOVERNMENT": // this value comes from azure cli
                return AzureEnvironment.AZURE_US_GOVERNMENT;
            default:
                return AzureEnvironment.AZURE;
        }
    }

    /**
     * Get the azure-secret.json file according to environment variable, the default location is $HOME/.azure/azure-secret.json
     */
    public static File getAzureSecretFile() {
        return new File(getAzureConfigFolder(), Constants.AZURE_SECRET_FILE);
    }


    /**
     * Get the azure config folder location, the default location is $HOME/.azure.
     */
    public static File getAzureConfigFolder() {
        return StringUtils.isNotBlank(System.getenv(Constants.AZURE_CONFIG_DIR)) ? new File(System.getenv(Constants.AZURE_CONFIG_DIR)) :
            Paths.get(System.getProperty(Constants.USER_HOME), Constants.AZURE_FOLDER).toFile();
    }


    /**
     * Check whether the azure-secret.json file exists and is not empty.
     */
    public static boolean existsAzureSecretFile() {
        final File azureSecretFile = getAzureSecretFile();
        return azureSecretFile.exists() && azureSecretFile.isFile() && azureSecretFile.length() > 0;
    }

    /**
     * Delete the azure-secret.json.
     *
     * @return true if the file is deleted.
     */
    public static boolean deleteAzureSecretFile() {
        if (existsAzureSecretFile()) {
            return FileUtils.deleteQuietly(getAzureSecretFile());
        }
        return false;
    }

    /***
     * Save the credential to a file.
     *
     * @param cred the credential
     * @param file the file name to save the credential
     * @throws IOException if there is any IO error.
     */
    public static void writeAzureCredentials(AzureCredential cred, File file) throws IOException {
        if (cred == null) {
            throw new IllegalArgumentException("Parameter 'cred' cannot be null.");
        }
        if (file == null) {
            throw new IllegalArgumentException("Parameter 'file' cannot be null.");
        }
        FileUtils.writeStringToFile(file, JsonUtils.toJson(cred), "utf8");
    }

    /***
     * Read the credential from default location.
     *
     * @return the saved credential
     * @throws IOException if there is any IO error.
     */
    public static AzureCredential readAzureCredentials() throws IOException {
        return readAzureCredentials(getAzureSecretFile());
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
     * Get azure token credential from $HOME/.azure/azure-secret.json(created by `mvn azure:login`)
     *
     * @return the azure token credential can be used in Azure SDK.
     * @throws IOException when there are some IO errors.
     */
    public static AzureTokenCredentials getMavenAzureLoginCredentials() throws IOException {
        final AzureCredential credentials = readAzureCredentials(getAzureSecretFile());
        final AzureEnvironment env = getAzureEnvironment(credentials.getEnvironment());
        return getMavenAzureLoginCredentials(credentials, env);
    }

    /**
     * Get azure credential from AzureCredential instance.
     *
     * @param credentials the azure credential
     * @param env the azure environment
     * @return the azure token credential can be used in Azure SDK.
     * @throws IOException when there are some IO errors.
     */
    public static AzureTokenCredentials getMavenAzureLoginCredentials(AzureCredential credentials, AzureEnvironment env) throws IOException {
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
                    writeAzureCredentials(credentials, getAzureSecretFile());
                } catch (InterruptedException | ExecutionException e) {
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
     * Get an AzureTokenCredentials from :
     * a. in-place &lt;auth&gt; configuration in pom.xml
     * b. $HOME/.azure/azure-secret.json(created by `mvn azure:login`)
     * c. cloud shell
     * d: $HOME/.azure/accessTokens.json(created by `az login`)
     *
     * @param configuration the in-place &lt;auth&gt; configuration in pom.xml
     * @return the azure credential through
     * @throws IOException when there are some IO errors.
     */
    public static AzureTokenCredentials getAzureTokenCredentials(AuthConfiguration configuration)
            throws InvalidConfigurationException, IOException {
        if (configuration != null) {
            return AzureServicePrincipleAuthHelper.getAzureServicePrincipleCredentials(configuration);
        }
        if (existsAzureSecretFile()) {
            try {
                return getMavenAzureLoginCredentials();
            } catch (IOException ex) {
                // ignore
            }
        }
        if (isInCloudShell()) {
            return new MSICredentials();
        }
        final File credentialParent = getAzureConfigFolder();
        if (credentialParent.exists() && credentialParent.isDirectory()) {
            final File azureProfile = new File(credentialParent, Constants.AZURE_PROFILE_NAME);
            final File accessTokens = new File(credentialParent, Constants.AZURE_TOKEN_NAME);

            if (azureProfile.exists() && accessTokens.exists()) {
                try {
                    final AzureCliCredentials azureCliCredentials = AzureCliCredentials.create(azureProfile, accessTokens);
                    if (azureCliCredentials.clientId() != null) {
                        return azureCliCredentials;
                    } else {
                        return AzureServicePrincipleAuthHelper.getCredentialFromAzureCliWithServicePrincipal();
                    }

                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        return null;
    }

    static boolean isInCloudShell() {
        return System.getenv(Constants.CLOUD_SHELL_ENV_KEY) != null;
    }

    private AzureAuthHelper() {

    }
}
