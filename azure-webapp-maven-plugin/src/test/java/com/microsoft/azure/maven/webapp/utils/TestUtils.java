/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import java.io.IOException;

public class TestUtils {
    public static void deleteAzureResouceGroup(String resourceGroupName, boolean waitForOperationFinish)
            throws InterruptedException, IOException {
        final String clientId = System.getenv("CLIENT_ID");
        final String tenantId = System.getenv("TENANT_ID");
        final String key = System.getenv("KEY");
        final String azLoginTemplate = "az login --service-principal -u %s -p %s --tenant %s";
        final String azDelteTemplate = "az group delete -y -n %s%s";

        final String[] commands = { String.format(azLoginTemplate, clientId, key, tenantId),
                String.format(azDelteTemplate, resourceGroupName, waitForOperationFinish ? "" : " --no-wait"),
                "az logout" };

        if (System.getProperty("os.name").contains("Windows")) {
            for (final String command : commands) {
                Runtime.getRuntime().exec(String.format("cmd /c %s", command)).waitFor();
            }
        } else {
            for (final String command : commands) {
                Runtime.getRuntime().exec(String.format("bash -c %s", command)).waitFor();
            }
        }
    }
}
