/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.maven;

import lombok.Getter;
import lombok.Setter;


public class MavenAzureCredential {
    @Setter
    @Getter
    private String accessTokenType;
    @Setter
    @Getter
    private String idToken;
    @Setter
    @Getter
    private String accessToken;
    @Setter
    @Getter
    private String refreshToken;
    private boolean isMultipleResourceRefreshToken;
    @Setter
    @Getter
    private String defaultSubscription;
    @Setter
    @Getter
    private String environment;

    public boolean isMultipleResourceRefreshToken() {
        return isMultipleResourceRefreshToken;
    }

    public void setMultipleResourceRefreshToken(boolean isMultipleResourceRefreshToken) {
        this.isMultipleResourceRefreshToken = isMultipleResourceRefreshToken;
    }
}
