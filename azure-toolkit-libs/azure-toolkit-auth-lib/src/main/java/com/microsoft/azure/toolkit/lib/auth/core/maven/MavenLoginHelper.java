/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.maven;

import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class MavenLoginHelper {
    // ClientId from https://github.com/Azure/azure-cli/blob/1beb6352ece2d06187bbccd66f1638f45b0340f7/src/azure-cli-core/azure/cli/core/_profile.py#L64
    private static final String AZURE_FOLDER = ".azure";
    private static final String USER_HOME = "user.home";
    private static final String AZURE_CONFIG_DIR = "AZURE_CONFIG_DIR";
    private static final String AZURE_SECRET_FILE = "azure-secret.json";

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
        JsonObject obj = JsonUtils.getGson().fromJson(jsonStr, JsonObject.class);
        if (obj == null) {
            return null;
        }

        AzureCredential credential = new AzureCredential();
        credential.setEnvironment(getJsonString(obj, "environment"));
        credential.setAccessToken(getJsonString(obj, "accessToken"));
        credential.setRefreshToken(getJsonString(obj, "refreshToken"));
        credential.setAccessTokenType(getJsonString(obj, "accessTokenType"));
        credential.setIdToken(getJsonString(obj, "idToken"));
        if (obj.has("userInfo")) {
            JsonObject userInfo = obj.get("userInfo").getAsJsonObject();
            AzureCredential.UserInfo ui = new AzureCredential.UserInfo();
            ui.setUniqueId(getJsonString(userInfo, "uniqueId"));
            ui.setDisplayableId(getJsonString(userInfo, "displayableId"));
            ui.setTenantId(getJsonString(userInfo, "tenantId"));
            credential.setUserInfo(ui);
        }
        if (obj.has("isMultipleResourceRefreshToken")) {
            credential.setMultipleResourceRefreshToken(obj.get("isMultipleResourceRefreshToken").getAsBoolean());
        }

        return credential;
    }

    private static String getJsonString(JsonObject json, String property) {
        if (json.has(property)) {
            return json.get(property).getAsString();
        }
        return null;
    }
}
