/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers.artifact;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.handlers.artifact.ZIPArtifactHandlerImpl;

import static com.microsoft.azure.common.function.Constants.APP_SETTING_WEBSITE_RUN_FROM_PACKAGE;

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
    public void publish(DeployTarget target) throws AzureExecutionException {
        FunctionArtifactHelper.updateAppSetting(target, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, RUN_FROM_PACKAGE_VALUE);
        try {
            // work around for issue https://dev.azure.com/msazure/Unified%20Platform%20KPIs/_workitems/edit/7481871
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            // Sorrow exception here as we just wait for 20s for kudu app settings update, which is not required in normal cases
        }
        super.publish(target);
    }
}
