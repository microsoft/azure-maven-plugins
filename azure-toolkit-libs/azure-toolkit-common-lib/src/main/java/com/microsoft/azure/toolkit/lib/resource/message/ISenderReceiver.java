/*
 *
 *  * Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.toolkit.lib.resource.message;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;

public interface ISenderReceiver extends AzResource {
    public void startReceivingMessage();
    public void stopReceivingMessage();
    public void sendMessage(String message);
    public boolean isListening();
    public boolean isSendEnabled();
}
