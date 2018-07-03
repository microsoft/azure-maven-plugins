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
def command2 = "az group delete -y -n maven-webapp-it-rg-11"
def command3 = "az logout"

if (System.properties['os.name'].contains('Windows')) {
    println "cmd /c ${command1}".execute().text
    println "cmd /c ${command2}".execute().text
    println "cmd /c ${command3}".execute().text
} else {
    println "bash -c ${command1}".execute().text
    println "bash -c ${command2}".execute().text
    println "bash -c ${command3}".execute().text
}