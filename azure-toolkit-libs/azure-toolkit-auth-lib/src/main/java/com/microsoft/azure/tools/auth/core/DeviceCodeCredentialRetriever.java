/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.common.util.TextUtils;
import org.apache.commons.lang3.StringUtils;

public class DeviceCodeCredentialRetriever extends AbstractCredentialRetriever {
    private static final String AZURE_TOOLKIT_CLIENT_ID = "777acee8-5286-4d6e-8b05-f7c851d8ed0a";
    private AzureEnvironment env;

    public DeviceCodeCredentialRetriever(AzureEnvironment env) {
        this.env = env;
    }

    public AzureCredentialWrapper retrieve() {
        DeviceCodeCredential deviceCodeCredential = new DeviceCodeCredentialBuilder().clientId(AZURE_TOOLKIT_CLIENT_ID)
                .challengeConsumer(challenge -> System.out.println(StringUtils.replace(challenge.getMessage(), challenge.getDeviceCode(),
                        TextUtils.blue(challenge.getDeviceCode())))).build();
        return new AzureCredentialWrapper(AuthMethod.DEVICE_CODE, deviceCodeCredential, env);
    }
}
