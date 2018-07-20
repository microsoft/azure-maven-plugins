/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.invoker;

import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommonUtils {
    private static final String clientId = System.getenv("CLIENT_ID");
    private static final String tenantId = System.getenv("TENANT_ID");
    private static final String key = System.getenv("KEY");

    private static final String loginAzureCli = "az login --service-principal -u %s -p %s --tenant %s";
    private static final String deleteResourceGroup = "az group delete -y -n %s%s";

    private static final String windowsCommand = "cmd /c %s";
    private static final String nonWindowsCommand = "bash -c %s";

    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");

    private static void azureLogin() throws IOException, InterruptedException {
        executeCommand(String.format(loginAzureCli, clientId, key, tenantId));
    }

    public static void deleteAzureResourceGroup(String resourceGroupName, boolean waitForOperationFinish)
            throws InterruptedException, IOException {
        executeCommand(
                String.format(deleteResourceGroup,
                        resourceGroupName,
                        waitForOperationFinish ? "" : " --no-wait"));
    }

    public static String executeCommand(final String command) throws IOException, InterruptedException {
        if (StringUtils.isNotEmpty(command)) {
            final String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
            final Process process = Runtime.getRuntime().exec(wholeCommand);
            process.waitFor();
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            final StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
        return "";
    }
}
