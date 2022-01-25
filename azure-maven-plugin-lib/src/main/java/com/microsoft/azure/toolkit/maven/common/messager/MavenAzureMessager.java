/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.maven.common.messager;

import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Log4j2
public class MavenAzureMessager implements IAzureMessager, IAzureMessage.ValueDecorator {
    @Override
    public boolean show(IAzureMessage message) {
        switch (message.getType()) {
            case ALERT:
            case CONFIRM:
            case WARNING:
                log.warn(message.getContent());
                return true;
            case ERROR:
                log.error(message.getContent(), ((Throwable) message.getPayload()));
                return true;
            case INFO:
            case SUCCESS:
            default:
                log.info(message.getContent());
                return true;
        }
    }

    @Override
    public String decorateValue(@Nonnull Object p, @Nullable IAzureMessage message) {
        return TextUtils.cyan(p.toString());
    }
}
