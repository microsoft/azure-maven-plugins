/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.artifact;

import static com.microsoft.azure.maven.function.Constants.APP_SETTING_WEBSITE_RUN_FROM_PACKAGE;

import java.io.IOException;

import com.microsoft.azure.maven.artifacthandler.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

import org.apache.maven.plugin.MojoExecutionException;

public class RunFromZipArtifactHandlerImpl extends ZIPArtifactHandlerImpl {

    protected static final String RUN_FROM_PACKAGE_VALUE = "1";

    public static class Builder extends ZIPArtifactHandlerImpl.Builder {
        @Override
        protected RunFromZipArtifactHandlerImpl.Builder self() {
            return this;
        }

        @Override
        public RunFromZipArtifactHandlerImpl build() {
            return new RunFromZipArtifactHandlerImpl(this);
        }
    }

    protected RunFromZipArtifactHandlerImpl(Builder builder) {
        super(builder);
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        FunctionArtifactHelper.updateAppSetting(target, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, RUN_FROM_PACKAGE_VALUE);
        super.publish(target);
    }
}
