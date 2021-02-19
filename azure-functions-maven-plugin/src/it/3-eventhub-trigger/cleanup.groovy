/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

import com.microsoft.azure.maven.function.invoker.storage.EventHubProcesser
import com.microsoft.azure.maven.function.invoker.CommonUtils

String storageName = "cihub${timestamp}"
String namespaceName = "FunctionCIEventHubNamespace-${timestamp}"
String resourceGroupName = "maven-functions-it-${timestamp}-rg-3"

EventHubProcesser eventHubProcesser = null
try {
    eventHubProcesser = new EventHubProcesser(resourceGroupName, namespaceName, storageName);
    // verify
    CommonUtils.runVerification(new Runnable() {
        @Override
        void run() {
            eventHubProcesser.sendMessageToEventHub("trigger", "CIInput")
            sleep(10 * 1000 /* ms */)
            assert eventHubProcesser.getMessageFromEventHub("output").get(0) == "CITest"
        }
    })
} finally {
    if (eventHubProcesser != null) {
        eventHubProcesser.close()
    }
}
CommonUtils.deleteAzureResourceGroup(resourceGroupName, false)
return true

