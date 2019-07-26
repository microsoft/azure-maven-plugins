/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.NotImplementedException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal to switch among multiple azure subscriptions.
 */
@Mojo(name = "select-subscription", inheritByDefault = true, aggregator = true)
public class SelectSubscriptionMojo extends AbstractMojo  {

    /**
     * The maven cli argument for set the active subscription by id or name
     */
    @Parameter(property = "subscription")
    private String subscription;

    @Override
    public void execute() throws MojoExecutionException {
        throw new NotImplementedException("Not implemented.");
    }

}
