/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import com.microsoft.azure.maven.webapp.utils.TestUtils;

TestUtils.deleteAzureResourceGroup("maven-webapp-it-rg-12", true)

TestUtils.createAzureWebApp("maven-webapp-it-rg-12", "westus",
        "maven-webapp-it-rg-12-service-plan", "maven-webapp-it-rg-12-web-app")

return true