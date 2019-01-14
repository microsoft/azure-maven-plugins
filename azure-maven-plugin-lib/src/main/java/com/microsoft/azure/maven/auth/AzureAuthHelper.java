/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.maven.Utils;
import com.microsoft.rest.LogLevel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import static com.microsoft.azure.maven.Utils.assureServerExist;

/**
 * Helper class to authenticate with Azure
 */
public class AzureAuthHelper {
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
    public static final String AUTH_WITH_MSI = "Detected in the Azure Cloud Shell, using MSI to authenticate.";
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



    protected AuthConfiguration config;

    /**
     * Constructor
     *
     * @param config
     */
    public AzureAuthHelper(final AuthConfiguration config) {
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
            final String subscriptionId = config.getSubscriptionId();
            return StringUtils.isEmpty(subscriptionId) ?
                    auth.withDefaultSubscription() :
                    auth.withSubscription(subscriptionId);
        } catch (Exception e) {
            getLog().debug(e);
        }
        return null;
    }

    private Log getLog() {
        return config.getLog();
    }

    protected LogLevel getLogLevel() {
        return getLog().isDebugEnabled() ?
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
            getLog().debug(SERVER_ID_NOT_CONFIG);
            return null;
        }

        final Server server = Utils.getServer(settings, serverId);
        try {
            assureServerExist(server, serverId);
        } catch (MojoExecutionException ex) {
            getLog().error(ex.getMessage());
            return null;
        }

        final ApplicationTokenCredentials credential = getAppTokenCredentialsFromServer(server);
        if (credential == null) {
            getLog().error(AZURE_AUTH_INVALID + serverId);
            return null;
        }

        final Authenticated auth = azureConfigure().authenticate(credential);
        if (auth != null) {
            getLog().info(AUTH_WITH_SERVER_ID + serverId);
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
            getLog().debug(AUTH_FILE_NOT_CONFIG);
            return null;
        }

        if (!authFile.exists()) {
            getLog().error(AUTH_FILE_NOT_EXIST + authFile.getAbsolutePath());
            return null;
        }

        try {
            final Authenticated auth = azureConfigure().authenticate(authFile);
            if (auth != null) {
                getLog().info(AUTH_WITH_FILE + authFile.getAbsolutePath());
            }
            return auth;
        } catch (Exception e) {
            getLog().error(AUTH_FILE_READ_FAIL + authFile.getAbsolutePath());
            getLog().error(e);
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
            Authenticated auth;
            if (isInCloudShell()) {
                getLog().info(AUTH_WITH_MSI);
                auth = azureConfigurable.authenticate(new MSICredentials());
            } else {
                getLog().info(AUTH_WITH_AZURE_CLI);
                auth = azureConfigurable.authenticate(AzureCliCredentials.create());
            }
            return auth;
        } catch (Exception e) {
            getLog().debug(AZURE_CLI_AUTH_FAIL);
            getLog().debug(e);
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

        final String clientId = Utils.getValueFromServerConfiguration(server, CLIENT_ID);
        if (StringUtils.isEmpty(clientId)) {
            getLog().debug(CLIENT_ID_NOT_CONFIG);
            return null;
        }

        final String tenantId = Utils.getValueFromServerConfiguration(server, TENANT_ID);
        if (StringUtils.isEmpty(tenantId)) {
            getLog().debug(TENANT_ID_NOT_CONFIG);
            return null;
        }

        final String environment = Utils.getValueFromServerConfiguration(server, ENVIRONMENT);
        final AzureEnvironment azureEnvironment = getAzureEnvironment(environment);
        getLog().debug("Azure Management Endpoint: " + azureEnvironment.managementEndpoint());

        final String key = Utils.getValueFromServerConfiguration(server, KEY);
        if (!StringUtils.isEmpty(key)) {
            getLog().debug(USE_KEY_TO_AUTH);
            return new ApplicationTokenCredentials(clientId, tenantId, key, azureEnvironment);
        } else {
            getLog().debug(KEY_NOT_CONFIG);
        }

        final String certificate = Utils.getValueFromServerConfiguration(server, CERTIFICATE);
        if (StringUtils.isEmpty(certificate)) {
            getLog().debug(CERTIFICATE_FILE_NOT_CONFIG);
            return null;
        }

        final String certificatePassword = Utils.getValueFromServerConfiguration(server, CERTIFICATE_PASSWORD);
        try {
            final byte[] cert;
            cert = Files.readAllBytes(Paths.get(certificate, new String[0]));
            getLog().debug(USE_CERTIFICATE_TO_AUTH + certificate);
            return new ApplicationTokenCredentials(clientId, tenantId, cert, certificatePassword, azureEnvironment);
        } catch (Exception e) {
            getLog().debug(CERTIFICATE_FILE_READ_FAIL + certificate);
        }

        return null;
    }

    private boolean isInCloudShell() {
        return System.getenv(CLOUD_SHELL_ENV_KEY) != null;
    }
}
