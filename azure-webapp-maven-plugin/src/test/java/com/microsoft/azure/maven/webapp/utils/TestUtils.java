/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import java.io.IOException;

public class TestUtils {
    private static final String clientId = System.getenv("CLIENT_ID");
    private static final String tenantId = System.getenv("TENANT_ID");
    private static final String key = System.getenv("KEY");

    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");

    private static void azureLogin() throws IOException, InterruptedException {
        final String azLoginTemplate = "az login --service-principal -u %s -p %s --tenant %s";
        Runtime.getRuntime().exec(formatCommand(azLoginTemplate, new String[]{clientId, key, tenantId})).waitFor();
    }

    private static void azureLogout() throws IOException, InterruptedException {
        Runtime.getRuntime().exec(formatCommand("%s", new String[]{"az logout"})).waitFor();
    }

    public static void deleteAzureResourceGroup(String resourceGroupName, boolean waitForOperationFinish)
            throws InterruptedException, IOException {

        final String azResourceGroupDeleteTemplate = "az group delete -y -n %s%s";
        azureLogin();
        Runtime.getRuntime().exec(formatCommand(azResourceGroupDeleteTemplate,
                new String[]{resourceGroupName, waitForOperationFinish ? "" : " --no-wait"})).waitFor();
        azureLogout();
    }

    public static void createWindowsAzureWebApp(final String resourceGroupName, final String location,
                                                final String servicePlanName, final String webAppName)
            throws IOException, InterruptedException {
        final String azResourceGroupCreateTemplate = "az group create -n %s --location %s";
        final String azAppServicePlanCreateTemplate = "az appservice plan create -g %s -n %s --sku S1";
        final String azWebAppCreateTemplate = "az webapp create -g %s -n %s --plan %s";

        azureLogin();
        final String[] commands = new String[]{
                formatCommand(azResourceGroupCreateTemplate, new String[]{resourceGroupName, location}),
                formatCommand(azAppServicePlanCreateTemplate, new String[]{resourceGroupName, servicePlanName}),
                formatCommand(azWebAppCreateTemplate, new String[]{resourceGroupName, webAppName, servicePlanName})
        };
        executeCommands(commands);
        azureLogout();
    }

    public static void createLinuxAzureWebApp(final String resourceGroupName, final String location,
                                              final String servicePlanName, final String webAppName,
                                              final String runtime) throws IOException, InterruptedException {
        final String azResourceGroupCreateTemplate = "az group create -g %s -l %s";
        final String azAppServicePlanCreateTemplate = "az appservice plan create -g %s -n %s --sku S1 --is-linux";
        final String azWebAppCreateTemplate = "az webapp create -g %s -n %s --plan %s --runtime \"%s\"";
        azureLogin();
        final String[] commands = new String[]{
                formatCommand(azResourceGroupCreateTemplate, new String[]{resourceGroupName, location}),
                formatCommand(azAppServicePlanCreateTemplate, new String[]{resourceGroupName, servicePlanName}),
                formatCommand(azWebAppCreateTemplate, new String[]{
                        resourceGroupName, webAppName, servicePlanName, runtime})
        };
        executeCommands(commands);
        azureLogout();
    }

    private static void executeCommands(final String[] commands) throws IOException, InterruptedException {
        if (commands != null && commands.length > 0) {
            for (final String command : commands) {
                Runtime.getRuntime().exec(command).waitFor();
            }
        }
    }

    private static String formatCommand(String template, String[] args) {
        if (isWindows) {
            return String.format("cmd /c %s", String.format(template, args));
        } else {
            return String.format("bash -c %s", String.format(template, args));
        }
    }
}
