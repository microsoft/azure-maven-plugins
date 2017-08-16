/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.function.AbstractFunctionMojo;

public class FTPArtifactHandlerImpl implements ArtifactHandler {
    public static final String DEFAULT_FUNCTION_ROOT = "/site/wwwroot";
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;

    private AbstractFunctionMojo mojo;

    public FTPArtifactHandlerImpl(final AbstractFunctionMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish() throws Exception {
        final FTPUploader uploader = getUploader();
        final FunctionApp app = mojo.getFunctionApp();
        final PublishingProfile profile = app.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        uploader.uploadDirectoryWithRetries(
                serverUrl,
                profile.ftpUsername(),
                profile.ftpPassword(),
                mojo.getDeploymentStageDirectory(),
                DEFAULT_FUNCTION_ROOT,
                DEFAULT_MAX_RETRY_TIMES);

        app.syncTriggers();
    }

    protected FTPUploader getUploader() {
        return new FTPUploader(mojo.getLog());
    }
}
