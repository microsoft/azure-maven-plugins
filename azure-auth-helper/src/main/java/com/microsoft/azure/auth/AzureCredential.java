/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.UserInfo;

public class AzureCredential {

    private String accessTokenType;
    private String idToken;
    private UserInfo userInfo;
    private String accessToken;
    private String refreshToken;
    private boolean isMultipleResourceRefreshToken;

    /***
     * Create an AzureCredential with {@link com.microsoft.aad.adal4j.AuthenticationResult}.
     *
     * @param result the AuthenticationResult, must not be null
     * @return the newly created AzureCredential
     */
    public static AzureCredential fromAuthenticationResult(AuthenticationResult result) {
        if (result == null) {
            throw new NullPointerException("result cannot be null");
        }
        final AzureCredential token = new AzureCredential();
        token.setAccessTokenType(result.getAccessTokenType());
        token.setAccessToken(result.getAccessToken());
        token.setRefreshToken(result.getRefreshToken());
        token.setIdToken(result.getIdToken());
        token.setUserInfo(result.getUserInfo());
        return token;
    }

    public String getAccessTokenType() {
        return accessTokenType;
    }

    public void setAccessTokenType(String accessTokenType) {
        this.accessTokenType = accessTokenType;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isMultipleResourceRefreshToken() {
        return isMultipleResourceRefreshToken;
    }

    public void setMultipleResourceRefreshToken(boolean isMultipleResourceRefreshToken) {
        this.isMultipleResourceRefreshToken = isMultipleResourceRefreshToken;
    }

    private AzureCredential() {

    }
}
