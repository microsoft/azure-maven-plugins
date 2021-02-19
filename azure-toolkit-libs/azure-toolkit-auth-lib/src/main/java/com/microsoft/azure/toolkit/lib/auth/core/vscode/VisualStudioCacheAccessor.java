/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.vscode;

import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.identity.CredentialUnavailableException;
import com.azure.identity.implementation.LinuxKeyRingAccessor;
import com.azure.identity.implementation.WindowsCredentialAccessor;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;
import com.sun.jna.Platform;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VisualStudioCacheAccessor {
    private static final String PLATFORM_NOT_SUPPORTED_ERROR = "Platform could not be determined for VS Code"
            + " credential authentication.";
    private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("^[-_.a-zA-Z0-9]+$");

    private static final String AZURE_RESOURCE_FILTER = "azure.resourceFilter";

    private final ClientLogger logger = new ClientLogger(VisualStudioCacheAccessor.class);

    private JsonNode getUserSettings() {
        JsonNode output;
        String homeDir = System.getProperty("user.home");
        String settingsPath = "";
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);
        mapper.configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
        try {
            if (Platform.isWindows()) {
                settingsPath = Paths.get(System.getenv("APPDATA"), "Code", "User", "settings.json")
                        .toString();
            } else if (Platform.isMac()) {
                settingsPath = Paths.get(homeDir, "Library",
                        "Application Support", "Code", "User", "settings.json").toString();
            } else if (Platform.isLinux()) {
                settingsPath = Paths.get(homeDir, ".config", "Code", "User", "settings.json")
                        .toString();
            } else {
                throw new CredentialUnavailableException(PLATFORM_NOT_SUPPORTED_ERROR);
            }
            File settingsFile = new File(settingsPath);
            output = mapper.readTree(settingsFile);
        } catch (Exception e) {
            return null;
        }
        return output;
    }

    /**
     * Get the user configured settings of Visual Studio code.
     *
     * @return a Map containing Vs Code user settings
     */
    public Map<String, String> getUserSettingsDetails() {
        JsonNode userSettings = getUserSettings();
        Map<String, String> details = new HashMap<>();

        String tenant = null;
        String cloud = "AzureCloud";

        if (userSettings != null && !userSettings.isNull()) {
            if (userSettings.has("azure.tenant")) {
                tenant = userSettings.get("azure.tenant").asText();
            }

            if (userSettings.has("azure.cloud")) {
                cloud = userSettings.get("azure.cloud").asText();
            }

            if (userSettings.has(AZURE_RESOURCE_FILTER)) {
                List<String> filteredSubscriptions = new ArrayList<>();
                for (JsonNode filter : userSettings.get(AZURE_RESOURCE_FILTER)) {
                    String[] tenantAndSubsId = StringUtils.split(filter.asText(), "/");
                    if (tenantAndSubsId.length == 2) {
                        filteredSubscriptions.add(tenantAndSubsId[1]);
                    } else {
                        logger.warning(String.format("Invalid 'azure.resourceFilter' settings '%s' in VSCode settings.json.", filter.asText()));
                    }
                }
                if (!filteredSubscriptions.isEmpty()) {
                    details.put("filter", StringUtils.join(filteredSubscriptions, ","));
                }

            }
        }

        if (!CoreUtils.isNullOrEmpty(tenant)) {
            details.put("tenant", tenant);
        }

        details.put("cloud", cloud);
        return details;
    }

    /**
     * Get the credential for the specified service and account name.
     *
     * @param serviceName the name of the service to lookup.
     * @param accountName the account of the service to lookup.
     * @return the credential.
     */
    public String getCredentials(String serviceName, String accountName) {
        String credential;

        if (Platform.isWindows()) {

            try {
                WindowsCredentialAccessor winCredAccessor =
                        new WindowsCredentialAccessor(serviceName, accountName);
                credential = winCredAccessor.read();
            } catch (Exception e) {
                throw new CredentialUnavailableException(
                        "Failed to read Vs Code credentials from Windows Credential API.", e);
            }

        } else if (Platform.isMac()) {

            try {
                KeyChainAccessor keyChainAccessor = new KeyChainAccessor(null,
                        serviceName, accountName);

                byte[] credentials = keyChainAccessor.read();
                credential = new String(credentials, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new CredentialUnavailableException(
                        "Failed to read Vs Code credentials from Mac Native Key Chain.", e);
            }

        } else if (Platform.isLinux()) {
            try {
                LinuxKeyRingAccessor keyRingAccessor = new LinuxKeyRingAccessor(
                        "org.freedesktop.Secret.Generic", "service",
                        serviceName, "account", accountName);

                byte[] credentials = keyRingAccessor.read();
                credential = new String(credentials, StandardCharsets.UTF_8);
            } catch (Exception | UnsatisfiedLinkError e) {
                throw new CredentialUnavailableException(
                        "Failed to read Vs Code credentials from Linux Key Ring.", e);
            }

        } else {
            throw new CredentialUnavailableException(PLATFORM_NOT_SUPPORTED_ERROR);
        }

        if (CoreUtils.isNullOrEmpty(credential) || !isRefreshTokenString(credential)) {
            throw new CredentialUnavailableException("Please authenticate via Azure Tools plugin in VS Code IDE.");
        }
        return credential;
    }

    private boolean isRefreshTokenString(String str) {
        return REFRESH_TOKEN_PATTERN.matcher(str).matches();
    }

}
