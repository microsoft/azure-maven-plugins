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
import java.util.concurrent.TimeUnit;

public class CommonUtils {
    private static final String clientId = System.getenv("CLIENT_ID");
    private static final String tenantId = System.getenv("TENANT_ID");
    private static final String key = System.getenv("KEY");

    private static final String loginAzureCli = "az login --service-principal -u %s -p %s --tenant %s";
    private static final String deleteResourceGroup = "az group delete -y -n %s%s";

    private static final String windowsCommand = "cmd /c %s";
    private static final String nonWindowsCommand = "bash -c %s";

    private static final int RETRY_TIMES = 5;
    private static final int WAIT_IN_SECOND = 5;

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

    /**
     * @param command input command
     * @return output of the process
     * @throws IOException
     * @throws InterruptedException
     */
    public static String executeCommand(final String command) throws IOException, InterruptedException {
        if (StringUtils.isNotEmpty(command)) {
            final String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
            final Process process = Runtime.getRuntime().exec(wholeCommand);
            process.waitFor();
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
        return "";
    }

    /**
     * @param verification the Runnable class which contains the verification logic
     * @throws Exception
     */
    public static void runVerification(Runnable verification) throws Exception {
        int i = 0;
        while (i < RETRY_TIMES) {
            try {
                verification.run();
                break;
            } catch (Exception e) {
                // ignore warm-up exception and wait for 5 seconds
                e.printStackTrace();
                i++;
                TimeUnit.SECONDS.sleep(WAIT_IN_SECOND);
            }
        }

        throw new Exception("Integration test fails for 5 times.");
    }
}
