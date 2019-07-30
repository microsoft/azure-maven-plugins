/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

public class Constants {
    // ClientId from https://github.com/Azure/azure-cli/blob/1beb6352ece2d06187bbccd66f1638f45b0340f7/src/azure-cli-core/azure/cli/core/_profile.py#L64
    public static final String CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";
    public static final String COMMON_TENANT = "common";

    public static final String AZURE_FOLDER = ".azure";
    public static final String USER_HOME = "user.home";
    public static final String AZURE_CONFIG_DIR = "AZURE_CONFIG_DIR";
    public static final String AZURE_SECRET_FILE = "azure-secret.json";
    public static final String AZURE_PROFILE_NAME = "azureProfile.json";
    public static final String AZURE_TOKEN_NAME = "accessTokens.json";

    public static final String CLOUD_SHELL_ENV_KEY = "ACC_CLOUD";

    private Constants() {

    }
}
