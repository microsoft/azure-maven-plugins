/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.function.invoker.CommonUtils
import com.microsoft.azure.maven.function.invoker.storage.EventHubProcesser

String functionName = "maven-functions-it-${timestamp}-3"
String storageName = "cihub${timestamp}"
String nameSpace = "FunctionCIEventHubNamespace-${timestamp}"
String resourceGroup = "maven-functions-it-${timestamp}-rg-3"

EventHubProcesser eventHubProcesser = null
try{
    eventHubProcesser = new EventHubProcesser(resourceGroup,nameSpace,storageName);
    eventHubProcesser.createOrGetEventHubByName("trigger")
    eventHubProcesser.createOrGetEventHubByName("output")
    // Get connnection string of EventHub and set it to trigger function
    def connectionString = eventHubProcesser.getEventHubConnectionString()
    CommonUtils.executeCommand("az webapp config appsettings set --name ${functionName} --resource-group ${resourceGroup} --settings CIEventHubConnection=\"${connectionString}\"")
    // trigger function
    eventHubProcesser.sendMessageToEventHub("trigger","CIInput")
    // verify
    CommonUtils.runVerification(new Runnable() {
        @Override
        void run() {
            sleep(10 * 1000 /* ms */)
            String msg = eventHubProcesser.getMessageFromEventHub("output").get(0)
            assert msg == "CITest"
        }
    })
}finally{
    if(eventHubProcesser!=null){
        eventHubProcesser.close()
    }
    CommonUtils.deleteAzureResourceGroup("maven-functions-it-${timestamp}-rg-3", false)
}
return true
