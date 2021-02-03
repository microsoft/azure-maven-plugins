/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core.vscode;

import com.azure.identity.CredentialUnavailableException;
import com.azure.identity.implementation.VisualStudioCacheAccessor;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.util.AzureEnvironmentUtils;
import com.sun.jna.Platform;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VisualStudioCodeProfileRetriever {
    private static final String PLATFORM_NOT_SUPPORTED_ERROR = "Platform could not be determined for VS Code credential authentication.";
    private static final String AZURE_CLOUD = "azure.cloud";
    private static final String AZURE_TENANT = "azure.tenant";
    private static final String TENANT = "tenant";
    private static final String CLOUD = "cloud";
    private static final String AZURE_RESOURCE_FILTER = "azure.resourceFilter";

    public static VisualStudioCodeAccountProfile getProfile(@Nonnull AzureEnvironment env) throws CredentialUnavailableException, InvalidConfigurationException, LoginFailureException {
        VisualStudioCacheAccessor accessor = new VisualStudioCacheAccessor();

        JsonNode userSettings = getUserSettings();
        Map<String, String> details = getUserSettingsDetails(userSettings);
        String cloud = details.get(CLOUD);
        if (StringUtils.isBlank(cloud)) {
            cloud = AzureEnvironmentUtils.getCloudNameForAzureCli(env);
        }

        try {
            if (StringUtils.isBlank(accessor.getCredentials("VS Code Azure", cloud))) {
                return null;
            }
        } catch (CredentialUnavailableException ex) {
            throw new LoginFailureException(String.format("Cannot get credentials from VSCode, " +
                    "please execute the VSCode command `Azure: Sign In` to login your VSCode, detailed error: %s", ex.getMessage()));
        }
        return getVsCodeAccountProfile(userSettings, cloud);
    }

    private static Map<String, String> getUserSettingsDetails(JsonNode userSettings) {
        Map<String, String> map = new HashMap<>();
        if (userSettings != null && !userSettings.isNull()) {
            if (userSettings.has(AZURE_CLOUD) && StringUtils.isNotBlank(userSettings.get(AZURE_CLOUD).asText())) {
                map.put(CLOUD, userSettings.get(AZURE_CLOUD).asText());
            }
            if (userSettings.has(AZURE_TENANT) && StringUtils.isNotBlank(userSettings.get(AZURE_TENANT).asText())) {
                map.put(TENANT, userSettings.get(AZURE_TENANT).asText());
            }
        }
        return map;
    }

    private static VisualStudioCodeAccountProfile getVsCodeAccountProfile(JsonNode userSettings, String cloud) throws InvalidConfigurationException {
        List<String> filteredSubscriptions = new ArrayList<>();

        if (userSettings.has(AZURE_RESOURCE_FILTER)) {
            for (JsonNode filter : userSettings.get(AZURE_RESOURCE_FILTER)) {
                String[] tenantAndSubsId = StringUtils.split(filter.asText(), "/");
                if (tenantAndSubsId.length == 2) {
                    filteredSubscriptions.add(tenantAndSubsId[1]);
                } else {
                    throw new InvalidConfigurationException(
                            String.format("Invalid 'azure.resourceFilter' settings '%s' in VSCode settings.json.", filter.asText()));
                }
            }
        }
        return new VisualStudioCodeAccountProfile(cloud, filteredSubscriptions.toArray(new String[0]));
    }

    /**
     * Code copied from https://github.com/Azure/azure-sdk-for-java/blob/master/sdk/identity/azure-identity/src/
     * main/java/com/azure/identity/implementation/VisualStudioCacheAccessor.java#L32
     * , since it doesn't support profile, we need to get the setting for user
     * selected subscription
     */
    private static JsonNode getUserSettings() {
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
            return mapper.readTree(settingsFile);
        } catch (IOException e) {
            return null;
        }
    }
}

