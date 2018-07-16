/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.deployadapter;

import com.microsoft.azure.management.appservice.PublishingProfile;

import java.io.File;

public interface IDeployTargetAdapter {
    String getName();
    String getType();
    String getDefaultHostName();
    void warDeploy(File war, String contextPath);
    PublishingProfile getPublishingProfile();
}
