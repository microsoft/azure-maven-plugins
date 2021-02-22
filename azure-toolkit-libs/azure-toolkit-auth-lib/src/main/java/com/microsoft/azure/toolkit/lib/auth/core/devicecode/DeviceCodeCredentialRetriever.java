/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.devicecode;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;

public class DeviceCodeCredentialRetriever extends AbstractCredentialRetriever {
    private static final String AZURE_TOOLKIT_CLIENT_ID = "777acee8-5286-4d6e-8b05-f7c851d8ed0a";

    public DeviceCodeCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() {
        DeviceCodeCredential deviceCodeCredential = new DeviceCodeCredentialBuilder().clientId(AZURE_TOOLKIT_CLIENT_ID)
            .challengeConsumer(challenge -> System.out.println(StringUtils.replace(challenge.getMessage(), challenge.getUserCode(),
                TextUtils.cyan(challenge.getUserCode())))).build();
        return new AzureCredentialWrapper(AuthMethod.DEVICE_CODE, deviceCodeCredential, getAzureEnvironment());
    }
}
