/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

// Verify Azure Functions
def url = "https://maven-functions-it-${timestamp}-1.azurewebsites.net/api/hello?json={\"body\":\"Azure\"}".toURL()

// Functions need some time to warm up
int i = 0
while (i < 5) {
    try {
        def response = url.getText()
        assert response == "Hello, Azure"
        break
    } catch (Exception e) {
        i++
        // ignore warm-up exception
    }
}

if (i >= 5) {
    throw new Exception("Integration test fail for Azure Functions http-trigger")
}

// Clean up resources created in test
def clientId = System.getenv("CLIENT_ID")
def tenantId = System.getenv("TENANT_ID")
def key = System.getenv("KEY")
def command = """
    az login --service-principal -u ${clientId}  -p ${key} --tenant ${tenantId}
    az group delete -y -n maven-functions-it-rg-1 --no-wait
    az logout
"""
def process = ["bash", "-c", command].execute()
println process.text
