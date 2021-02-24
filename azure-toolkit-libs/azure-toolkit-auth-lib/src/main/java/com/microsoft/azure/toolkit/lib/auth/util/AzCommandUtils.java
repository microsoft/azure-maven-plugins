/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.util;

import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.util.CoreUtils;
import com.azure.identity.CredentialUnavailableException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class AzCommandUtils {
    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");
    private static final String WINDOWS_STARTER = "cmd.exe";
    private static final String LINUX_MAC_STARTER = "/bin/sh";
    private static final String WINDOWS_SWITCHER = "/c";
    private static final String LINUX_MAC_SWITCHER = "-c";
    private static final String DEFAULT_WINDOWS_SYSTEM_ROOT = System.getenv("SystemRoot");
    private static final String DEFAULT_MAC_LINUX_PATH = "/bin/";
    private static final String WINDOWS_PROCESS_ERROR_MESSAGE = "'az' is not recognized";
    private static final String LINUX_MAC_PROCESS_ERROR_MESSAGE = "(.*)az:(.*)not found";

    /**
     * Modified code based on https://github.com/Azure/azure-sdk-for-java/blob/master
     * /sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClient.java#L411
     *
     * @param command the az command to be executed
     */
    public static JsonElement executeAzCommandJson(String command) {
        BufferedReader reader = null;
        try {
            final String starter;
            final String switcher;
            if (isWindows) {
                starter = WINDOWS_STARTER;
                switcher = WINDOWS_SWITCHER;
            } else {
                starter = LINUX_MAC_STARTER;
                switcher = LINUX_MAC_SWITCHER;
            }

            final ProcessBuilder builder = new ProcessBuilder(starter, switcher, command);
            final String workingDirectory = getSafeWorkingDirectory();
            if (workingDirectory != null) {
                builder.directory(new File(workingDirectory));
            } else {
                throw new IllegalStateException("A Safe Working directory could not be found to execute command from.");
            }
            builder.redirectErrorStream(true);
            final Process process = builder.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            final StringBuilder output = new StringBuilder();
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith(WINDOWS_PROCESS_ERROR_MESSAGE) || line.matches(LINUX_MAC_PROCESS_ERROR_MESSAGE)) {
                    throw new CredentialUnavailableException(
                            "AzureCliTenantCredential authentication unavailable. Azure CLI not installed.");
                }
                output.append(line);
            }

            final String processOutput = output.toString();
            process.waitFor(10, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                if (processOutput.length() > 0) {
                    final String redactedOutput = redactInfo("\"accessToken\": \"(.*?)(\"|$)", processOutput);
                    if (redactedOutput.contains("az login") || redactedOutput.contains("az account set")) {
                        throw new CredentialUnavailableException(
                                "AzureCliTenantCredential authentication unavailable. Please run 'az login' to set up account.");
                    }
                    throw new ClientAuthenticationException(redactedOutput, null);
                } else {
                    throw new ClientAuthenticationException("Failed to invoke Azure CLI ", null);
                }
            }

            if (StringUtils.startsWith(StringUtils.trim(processOutput), "[")) {
                return JsonUtils.getGson().fromJson(output.toString(), JsonArray.class);
            } else {
                return JsonUtils.getGson().fromJson(output.toString(), JsonObject.class);
            }

        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                new IllegalStateException(ex);
            }
        }
    }

    private static String getSafeWorkingDirectory() {
        if (isWindows) {
            if (CoreUtils.isNullOrEmpty(DEFAULT_WINDOWS_SYSTEM_ROOT)) {
                return null;
            }
            return DEFAULT_WINDOWS_SYSTEM_ROOT + "\\system32";
        } else {
            return DEFAULT_MAC_LINUX_PATH;
        }
    }

    private static String redactInfo(String regex, String input) {
        return input.replaceAll(regex, "****");
    }
}
