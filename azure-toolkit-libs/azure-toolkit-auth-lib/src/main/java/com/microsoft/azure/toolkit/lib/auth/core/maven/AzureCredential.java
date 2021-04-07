/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.maven;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
class AzureCredential {

    private String accessTokenType;
    private String idToken;
    private UserInfo userInfo;
    private String accessToken;
    private String refreshToken;
    private boolean isMultipleResourceRefreshToken;
    private String defaultSubscription;
    private String environment;


    @Setter
    @Getter
    static class UserInfo {
        String uniqueId;
        String displayableId;
        String tenantId;
    }
}
