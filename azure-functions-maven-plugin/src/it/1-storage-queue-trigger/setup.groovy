/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

import com.microsoft.azure.maven.function.invoker.CommonUtils

CommonUtils.azureLogin()

CommonUtils.deleteAzureResourceGroup("maven-functions-it-${timestamp}-rg-1", true)

return true
