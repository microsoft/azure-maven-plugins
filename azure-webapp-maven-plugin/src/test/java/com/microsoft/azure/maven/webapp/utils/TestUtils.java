/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;

public class TestUtils {
    private static final String clientId = System.getenv("CLIENT_ID");
    private static final String tenantId = System.getenv("TENANT_ID");
    private static final String key = System.getenv("KEY");

    private static final String deleteResourceGroup = "az group delete -y -n %s%s";
    private static final String loginAzureCli = "az login --service-principal -u %s -p %s --tenant %s";
    private static final String createResourceGroup = "az group create -n %s --location %s";
    private static final String createAppServicePlan = "az appservice plan create -g %s -n %s --sku S1";
    private static final String createWebApp = "az webapp create -g %s -p %s -n %s";
    private static final String createLinuxWebApp = createWebApp + " --runtime \"TOMCAT|8.5-jre8\"";
    private static final String createLinuxAppServicePlan = createAppServicePlan + " --is-linux";

    private static final String windowsCommand = "cmd /c %s";
    private static final String nonWindowsCommand = "bash -c %s";

    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");

    public static void azureLogin() throws IOException, InterruptedException {
        executeCommand(String.format(loginAzureCli, clientId, key, tenantId));
    }

    public static void deleteAzureResourceGroup(String resourceGroupName, boolean waitForOperationFinish)
            throws InterruptedException, IOException {

        executeCommand(
                String.format(deleteResourceGroup,
                        resourceGroupName,
                        waitForOperationFinish ? "" : " --no-wait"));
    }

    private static void createWebApp(final String resourceGroupName, final String location,
                                     final String servicePlanName, final String webAppName,
                                     final boolean isLinux) throws IOException, InterruptedException {

        executeCommand(
                String.format(createResourceGroup,
                        resourceGroupName,
                        location));
        executeCommand(
                String.format(isLinux ? createLinuxAppServicePlan : createAppServicePlan,
                        resourceGroupName,
                        servicePlanName));
        executeCommand(
                String.format(isLinux ? createLinuxWebApp : createWebApp,
                        resourceGroupName,
                        servicePlanName,
                        webAppName));

    }

    private static void executeCommand(final String command) throws IOException, InterruptedException {
        if (StringUtils.isNotEmpty(command)) {
            final String wholeCommand = String.format(isWindows ? windowsCommand : nonWindowsCommand, command);
            Runtime.getRuntime().exec(wholeCommand).waitFor();
        }
    }
}
