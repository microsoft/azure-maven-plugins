/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.auth.AzureAuthHelper;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

@Mojo(name = "logout", inheritByDefault = true, aggregator = true)
public class LogoutMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();
        final File azureSecretFile = AzureAuthHelper.getAzureSecretFile();
        if (AzureAuthHelper.existsAzureSecretFile()) {
            FileUtils.deleteQuietly(azureSecretFile);
            log.info("You have logged out successfully.");
        } else {
            log.warn("You are not logged in.");
        }
    }

}
