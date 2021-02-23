/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.devicecode;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.MsalToken;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.toolkit.lib.auth.core.IAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.common.CommonAccountEntityBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.common.MsalTokenBuilder;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

@AllArgsConstructor
public class DeviceCodeAccountEntityBuilder implements IAccountEntityBuilder {
    private static final String AZURE_TOOLKIT_CLIENT_ID = "777acee8-5286-4d6e-8b05-f7c851d8ed0a";
    private AzureEnvironment environment;

    @Override
    public AccountEntity build() {
        AccountEntity accountEntity = CommonAccountEntityBuilder.createAccountEntity(AuthMethod.DEVICE_CODE);
        accountEntity.setEnvironment(this.environment);
        try {
            MsalToken msalToken = new MsalTokenBuilder(environment, AZURE_TOOLKIT_CLIENT_ID).buildDeviceCode(
                    challenge -> System.out.println(StringUtils.replace(challenge.getMessage(), challenge.getUserCode(),
                    TextUtils.cyan(challenge.getUserCode())))).block();
            IAuthenticationResult result = msalToken.getAuthenticationResult();
            if (result != null && result.account() != null) {
                accountEntity.setEmail(result.account().username());
            }
            String refreshToken = (String) FieldUtils.readField(result, "refreshToken", true);
            if (StringUtils.isBlank(refreshToken)) {
                throw new LoginFailureException("Cannot get refresh token from device code login workflow.");
            }

            accountEntity.setCredentialBuilder(CommonAccountEntityBuilder.fromRefreshToken(environment, AZURE_TOOLKIT_CLIENT_ID, refreshToken));
            CommonAccountEntityBuilder.listTenants(accountEntity);
            accountEntity.setAuthenticated(true);
        } catch (Throwable ex) {
            accountEntity.setAuthenticated(false);
            accountEntity.setError(ex);
        }
        return accountEntity;
    }
}
