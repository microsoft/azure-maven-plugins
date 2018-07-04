/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

// Delete resources before test
def clientId = System.getenv("CLIENT_ID")
def tenantId = System.getenv("TENANT_ID")
def key = System.getenv("KEY")

def command1 = "az login --service-principal -u ${clientId}  -p ${key} --tenant ${tenantId}"
def command2 = "az group delete -y -n maven-webapp-it-rg-3"
def command3 = "az logout"

def commands = [command1, command2, command3]

if (System.properties['os.name'].contains('Windows')) {
    for (c in commands) {
        println "cmd /c ${c}".execute().text
    }
} else {
    for (c in commands) {
        println "bash -c ${c}".execute().text
    }
}