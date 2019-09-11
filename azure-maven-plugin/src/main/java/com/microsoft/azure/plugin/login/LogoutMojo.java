/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.auth.AzureAuthHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "logout", aggregator = true)
public class LogoutMojo extends AbstractAzureMojo {

    @Override
    public void doExecute() {
        final Log log = getLog();
        if (AzureAuthHelper.deleteAzureSecretFile()) {
            log.info("You have logged out successfully.");
        } else {
            log.warn("You are not logged in.");
        }
    }

}
