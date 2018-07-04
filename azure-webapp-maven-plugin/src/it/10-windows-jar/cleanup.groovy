/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

// Clean up resources created in test
def clientId = System.getenv("CLIENT_ID")
def tenantId = System.getenv("TENANT_ID")
def key = System.getenv("KEY")
def commands = [
    "az login --service-principal -u ${clientId}  -p ${key} --tenant ${tenantId}",
    "az group delete -y -n maven-webapp-it-rg-10 --no-wait",
    "az logout"
]

if (System.properties['os.name'].contains('Windows')) {
    for (c in commands) {
       "cmd /c ${c}".execute().text
    }
} else {
    for (c in commands) {
        "bash -c ${c}".execute().text
    }
}