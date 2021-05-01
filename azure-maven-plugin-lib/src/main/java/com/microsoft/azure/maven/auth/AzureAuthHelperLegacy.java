/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.rest.LogLevel;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Scanner;

/**
 * Helper class to authenticate with Azure
 */
public class AzureAuthHelperLegacy {
    public static final String CLIENT_ID = "client";
    public static final String TENANT_ID = "tenant";
    public static final String KEY = "key";
    public static final String CERTIFICATE = "certificate";
    public static final String CERTIFICATE_PASSWORD = "certificatePassword";
    public static final String ENVIRONMENT = "environment";
    public static final String CLOUD_SHELL_ENV_KEY = "ACC_CLOUD";

    public static final String AUTH_WITH_SERVER_ID = "Authenticate with ServerId: ";
    public static final String AUTH_WITH_FILE = "Authenticate with file: ";
    public static final String AUTH_WITH_AZURE_CLI = "Authenticate with Azure CLI 2.0";
    public static final String AUTH_WITH_MSI = "In the Azure Cloud Shell, use MSI to authenticate.";
    public static final String USE_KEY_TO_AUTH = "Use key to get Azure authentication token: ";
    public static final String USE_CERTIFICATE_TO_AUTH = "Use certificate to get Azure authentication token.";

    public static final String SERVER_ID_NOT_CONFIG = "ServerId is not configured for Azure authentication.";
    public static final String CLIENT_ID_NOT_CONFIG = "Client Id of your service principal is not configured.";
    public static final String TENANT_ID_NOT_CONFIG = "Tenant Id of your service principal is not configured.";
    public static final String KEY_NOT_CONFIG = "Key of your service principal is not configured.";
    public static final String CERTIFICATE_FILE_NOT_CONFIG = "Certificate of your service principal is not configured.";
    public static final String CERTIFICATE_FILE_READ_FAIL = "Failed to read certificate file: ";
    public static final String AZURE_AUTH_INVALID = "Authentication info for Azure is not valid. ServerId=";
    public static final String AUTH_FILE_NOT_CONFIG = "Authentication file is not configured.";
    public static final String AUTH_FILE_NOT_EXIST = "Authentication file does not exist: ";
    public static final String AUTH_FILE_READ_FAIL = "Failed to read authentication file: ";
    public static final String AZURE_CLI_AUTH_FAIL = "Failed to authenticate with Azure CLI 2.0";
    public static final String AZURE_CLI_GET_SUBSCRIPTION_FAIL = "Failed to get default subscription of Azure CLI, " +
            "please login Azure CLI first.";
    public static final String AZURE_CLI_LOAD_TOKEN_FAIL = "Failed to load Azure CLI token file, " +
            "please login Azure CLI first.";

    private static final String AZURE_FOLDER = ".azure";
    private static final String AZURE_PROFILE_NAME = "azureProfile.json";
    private static final String AZURE_TOKEN_NAME = "accessTokens.json";

    protected AuthConfiguration config;

    /**
     * Constructor
     *
     * @param config
     */
    public AzureAuthHelperLegacy(final AuthConfiguration config) {
        if (config == null) {
            throw new NullPointerException();
        }
        this.config = config;
    }

    public Azure getAzureClient() {
        final Authenticated auth = getAuthObj();
        if (auth == null) {
            return null;
        }

        try {
            String subscriptionId = config.getSubscriptionId();
            // For cloud shell, use subscription in profile as the default subscription.
            if (StringUtils.isEmpty(subscriptionId) && isInCloudShell()) {
                subscriptionId = getSubscriptionOfCloudShell();
            }
            return StringUtils.isEmpty(subscriptionId) ?
                    auth.withDefaultSubscription() :
                    auth.withSubscription(subscriptionId);
        } catch (Exception e) {
            Log.debug(e.getMessage());
        }
        return null;
    }

    protected LogLevel getLogLevel() {
        return Log.isDebugEnabled() ?
                LogLevel.BODY_AND_HEADERS :
                LogLevel.NONE;
    }

    protected Azure.Configurable azureConfigure() {
        final String httpProxyHost = config.getHttpProxyHost();
        final int httpProxyPort = config.getHttpProxyPort();
        final Azure.Configurable configurable = Azure.configure()
                .withLogLevel(getLogLevel())
                .withUserAgent(config.getUserAgent());

        return StringUtils.isNotEmpty(httpProxyHost) ?
            configurable.withProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort))) :
            configurable;
    }

    protected AzureEnvironment getAzureEnvironment(String environment) {
        if (StringUtils.isEmpty(environment)) {
            return AzureEnvironment.AZURE;
        }

        switch (environment.toUpperCase(Locale.ENGLISH)) {
            case "AZURE_CHINA":
                return AzureEnvironment.AZURE_CHINA;
            case "AZURE_GERMANY":
                return AzureEnvironment.AZURE_GERMANY;
            case "AZURE_US_GOVERNMENT":
                return AzureEnvironment.AZURE_US_GOVERNMENT;
            default:
                return AzureEnvironment.AZURE;
        }
    }

    protected Authenticated getAuthObj() {
        Authenticated auth;
        final AuthenticationSetting authSetting = config.getAuthenticationSetting();
        if (authSetting != null) {
            auth = getAuthObjFromServerId(config.getSettings(), authSetting.getServerId());
            if (auth == null) {
                auth = getAuthObjFromFile(authSetting.getFile());
            }
        } else {
            auth = getAuthObjFromAzureCli();
        }
        return auth;
    }

    /**
     * Get Authenticated object by referencing server definition in Maven settings.xml
     *
     * @param settings Settings object
     * @param serverId Server Id to search in settings.xml
     * @return Authenticated object if configurations are correct; otherwise return null.
     */
    protected Authenticated getAuthObjFromServerId(final Settings settings, final String serverId) {
        if (StringUtils.isEmpty(serverId)) {
            Log.debug(SERVER_ID_NOT_CONFIG);
            return null;
        }

        final Server server = getServer(settings, serverId);
        try {
            assureServerExist(server, serverId);
        } catch (AzureExecutionException ex) {
            Log.error(ex);
            return null;
        }

        final ApplicationTokenCredentials credential = getAppTokenCredentialsFromServer(server);
        if (credential == null) {
            Log.error(AZURE_AUTH_INVALID + serverId);
            return null;
        }

        final Authenticated auth = azureConfigure().authenticate(credential);
        if (auth != null) {
            Log.info(AUTH_WITH_SERVER_ID + serverId);
        }
        return auth;
    }

    /**
     * Get Authenticated object using file.
     *
     * @param authFile Authentication file object.
     * @return Authenticated object of file is valid; otherwise return null.
     */
    protected Authenticated getAuthObjFromFile(final File authFile) {
        if (authFile == null) {
            Log.debug(AUTH_FILE_NOT_CONFIG);
            return null;
        }

        if (!authFile.exists()) {
            Log.error(AUTH_FILE_NOT_EXIST + authFile.getAbsolutePath());
            return null;
        }

        try {
            final Authenticated auth = azureConfigure().authenticate(authFile);
            if (auth != null) {
                Log.info(AUTH_WITH_FILE + authFile.getAbsolutePath());
            }
            return auth;
        } catch (Exception e) {
            Log.error(AUTH_FILE_READ_FAIL + authFile.getAbsolutePath());
            Log.error(e);
        }
        return null;
    }

    /**
     * Get Authenticated object using authentication file from Azure CLI 2.0
     *
     * Note: The integrated Azure CLI in Azure Cloud Shell does not have the accessToken.json,
     * so we need to use MSI to authenticate in the Cloud Shell.
     *
     * @return Authenticated object if Azure CLI 2.0 is logged in correctly; otherwise return null.
     */
    protected Authenticated getAuthObjFromAzureCli() {
        try {
            final Azure.Configurable azureConfigurable = azureConfigure();
            final Authenticated auth;
            if (isInCloudShell()) {
                Log.info(AUTH_WITH_MSI);
                auth = azureConfigurable.authenticate(new MSICredentials());
            } else {
                Log.info(AUTH_WITH_AZURE_CLI);
                final AzureCliCredentials azureCliCredentials = AzureCliCredentials.create();
                if (azureCliCredentials.clientId() != null) {
                    return azureConfigurable.authenticate(azureCliCredentials);
                } else {
                    final ApplicationTokenCredentials servicePrincipalCredentials =
                            getCredentialFromAzureCliWithServicePrincipal();
                    return servicePrincipalCredentials == null ? null :
                            azureConfigurable.authenticate(getCredentialFromAzureCliWithServicePrincipal());
                }
            }
            return auth;
        } catch (Exception e) {
            Log.debug(AZURE_CLI_AUTH_FAIL);
            Log.debug(e.getMessage());
        }
        return null;
    }

    /**
     * Get Authenticated object from token file of Azure CLI 2.0 logged with Service Principal
     *
     * Note: This is a workaround for issue https://github.com/microsoft/azure-maven-plugins/issues/125
     *
     * @return Authenticated object if Azure CLI 2.0 is logged with Service Principal.
     */
    protected ApplicationTokenCredentials getCredentialFromAzureCliWithServicePrincipal() throws IOException {
        final JsonObject subscription = getDefaultSubscriptionObject();
        final String servicePrincipalName = subscription == null ? null : subscription.get("user")
                .getAsJsonObject().get("name").getAsString();
        if (servicePrincipalName == null) {
            Log.error(AZURE_CLI_GET_SUBSCRIPTION_FAIL);
            return null;
        }
        final JsonArray tokens = getAzureCliTokenList();
        if (tokens == null) {
            Log.error(AZURE_CLI_LOAD_TOKEN_FAIL);
            return null;
        }
        for (final JsonElement token : tokens) {
            final JsonObject tokenObject = (JsonObject) token;
            if (tokenObject.has("servicePrincipalId") &&
                    tokenObject.get("servicePrincipalId").getAsString().equals(servicePrincipalName)) {
                final String tenantId = tokenObject.get("servicePrincipalTenant").getAsString();
                final String key = tokenObject.get("accessToken").getAsString();
                return new ApplicationTokenCredentials(servicePrincipalName, tenantId, key, getAzureEnvironment(null));
            }
        }
        return null;
    }

    /**
     * Get ApplicationTokenCredentials from server definition in Maven settings.xml
     *
     * @param server Server object from settings.xml
     * @return ApplicationTokenCredentials object if configuration is correct; otherwise return null.
     */
    protected ApplicationTokenCredentials getAppTokenCredentialsFromServer(Server server) {
        if (server == null) {
            return null;
        }

        final String clientId = getValueFromServerConfiguration(server, CLIENT_ID);
        if (StringUtils.isEmpty(clientId)) {
            Log.debug(CLIENT_ID_NOT_CONFIG);
            return null;
        }

        final String tenantId = getValueFromServerConfiguration(server, TENANT_ID);
        if (StringUtils.isEmpty(tenantId)) {
            Log.debug(TENANT_ID_NOT_CONFIG);
            return null;
        }

        final String environment = getValueFromServerConfiguration(server, ENVIRONMENT);
        final AzureEnvironment azureEnvironment = getAzureEnvironment(environment);
        Log.debug("Azure Management Endpoint: " + azureEnvironment.managementEndpoint());

        final String key = getValueFromServerConfiguration(server, KEY);
        if (!StringUtils.isEmpty(key)) {
            Log.debug(USE_KEY_TO_AUTH);
            return new ApplicationTokenCredentials(clientId, tenantId, key, azureEnvironment);
        } else {
            Log.debug(KEY_NOT_CONFIG);
        }

        final String certificate = getValueFromServerConfiguration(server, CERTIFICATE);
        if (StringUtils.isEmpty(certificate)) {
            Log.debug(CERTIFICATE_FILE_NOT_CONFIG);
            return null;
        }

        final String certificatePassword = getValueFromServerConfiguration(server, CERTIFICATE_PASSWORD);
        try {
            final byte[] cert;
            cert = Files.readAllBytes(Paths.get(certificate, new String[0]));
            Log.debug(USE_CERTIFICATE_TO_AUTH + certificate);
            return new ApplicationTokenCredentials(clientId, tenantId, cert, certificatePassword, azureEnvironment);
        } catch (Exception e) {
            Log.debug(CERTIFICATE_FILE_READ_FAIL + certificate);
        }

        return null;
    }

    public static boolean isInCloudShell() {
        return System.getenv(CLOUD_SHELL_ENV_KEY) != null;
    }

    public static String getSubscriptionOfCloudShell() throws IOException {
        final JsonObject subscription = getDefaultSubscriptionObject();
        return subscription == null ? null : subscription.getAsJsonPrimitive("id").getAsString();
    }

    private static JsonObject getDefaultSubscriptionObject() throws IOException {
        final File azureProfile = Paths.get(System.getProperty("user.home"),
                AZURE_FOLDER, AZURE_PROFILE_NAME).toFile();
        try (final FileInputStream fis = new FileInputStream(azureProfile);
             final Scanner scanner = new Scanner(new BOMInputStream(fis))) {
            final String jsonProfile = scanner.useDelimiter("\\Z").next();
            final JsonArray subscriptionList = (new Gson()).fromJson(jsonProfile, JsonObject.class)
                    .getAsJsonArray("subscriptions");
            for (final JsonElement child : subscriptionList) {
                final JsonObject subscription = (JsonObject) child;
                if (subscription.getAsJsonPrimitive("isDefault").getAsBoolean()) {
                    return subscription;
                }
            }
        }
        return null;
    }

    private static JsonArray getAzureCliTokenList() throws IOException {
        final File azureProfile = Paths.get(System.getProperty("user.home"),
                AZURE_FOLDER, AZURE_TOKEN_NAME).toFile();
        try (final FileInputStream fis = new FileInputStream(azureProfile);
             final Scanner scanner = new Scanner(new BOMInputStream(fis))) {
            final String jsonProfile = scanner.useDelimiter("\\Z").next();
            return (new Gson()).fromJson(jsonProfile, JsonArray.class);
        }
    }

    /**
     * Get server credential from Maven settings by server Id.
     *
     * @param settings Maven settings object.
     * @param serverId Server Id.
     * @return Server object if it exists in settings. Otherwise return null.
     */
    private static Server getServer(final Settings settings, final String serverId) {
        if (settings == null || StringUtils.isEmpty(serverId)) {
            return null;
        }
        return settings.getServer(serverId);
    }

    /**
     * Assure the server with specified id does exist in settings.xml.
     * It could be the server used for azure authentication.
     * Or, the server used for docker hub authentication of runtime configuration.
     * @param server
     * @param serverId
     * @throws AzureExecutionException
     */
    private static void assureServerExist(final Server server, final String serverId) throws AzureExecutionException {
        if (server == null) {
            throw new AzureExecutionException(String.format("Server not found in settings.xml. ServerId=%s", serverId));
        }
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param server Server object.
     * @param key    Key string.
     * @return String value if key exists; otherwise, return null.
     */
    private static String getValueFromServerConfiguration(final Server server, final String key) {
        if (server == null) {
            return null;
        }

        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        final Xpp3Dom node = configuration.getChild(key);
        if (node == null) {
            return null;
        }

        return node.getValue();
    }
}
