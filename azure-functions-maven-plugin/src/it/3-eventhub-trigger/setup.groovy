/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.mstest.CommonUtils
import com.mstest.EventHubProcesser

String functionName = "eventhubtrigger-verify"
String storageName = "mavenverifycihub"
String namespaceName = "FunctionCIEventHubNamespace-verify"
String resourceGroupName = "eventhubtrigger-verify-group"

CommonUtils.azureLogin()
CommonUtils.deleteAzureResourceGroup(resourceGroupName, true)

// Create EventHub
EventHubProcesser eventHubProcesser = null
eventHubProcesser = new EventHubProcesser(resourceGroupName, namespaceName, storageName);
eventHubProcesser.createOrGetEventHubByName("trigger")
eventHubProcesser.createOrGetEventHubByName("output")

// Get connnection string of EventHub and save it to pom
def connectionString = eventHubProcesser.getEventHubConnectionString()

def pomFile = "./pom.xml"
def pomObject = new XmlParser().parse(pomFile)
def pluginNode = pomObject.build.plugins.plugin.find { it.artifactId.text() == '@project.artifactId@' }
pluginNode.configuration.appSettings.property.find {
    it.name.text() == "CIEventHubConnection"
}.value[0].value = connectionString

def writer = new FileWriter(pomFile)
def printer = new XmlNodePrinter(new PrintWriter(writer))
printer.preserveWhitespace = true
printer.print(pomObject)

return true