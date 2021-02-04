/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class PublishingProfile {
    private String ftpUrl;
    private String ftpUsername;
    private String ftpPassword;
    private String gitUrl;
    private String gitUsername;
    private String gitPassword;

    public static PublishingProfile createFromServiceModel(com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile) {
        return builder()
                .ftpUrl(publishingProfile.ftpUrl())
                .ftpUsername(publishingProfile.ftpUsername())
                .ftpPassword(publishingProfile.ftpPassword())
                .gitUrl(publishingProfile.gitUrl())
                .gitUsername(publishingProfile.gitUsername())
                .gitPassword(publishingProfile.gitPassword()).build();
    }
}
