/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.webapp.utils.TestUtils

TestUtils.deleteAzureResourceGroup("maven-webapp-it-rg-13", true)

TestUtils.createLinuxAzureWebApp("maven-webapp-it-rg-13", "westus",
        "maven-webapp-it-rg-13-service-plan",
        "maven-webapp-it-rg-13-webapp-linux", "TOMCAT|8.5-jre8")

return true