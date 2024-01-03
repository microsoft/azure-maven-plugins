/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.maven.common.messager;

import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager.DummyOpenUrlAction;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessagerProvider;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

@Slf4j
public class MavenAzureMessager implements IAzureMessager, IAzureMessage.ValueDecorator {
    @Override
    public boolean show(IAzureMessage message) {
        final Stream<DummyOpenUrlAction> actions = Arrays.stream(ObjectUtils.firstNonNull(message.getActions(), new Action[0]))
            .filter(action -> action instanceof DummyOpenUrlAction).map(a -> ((DummyOpenUrlAction) a));
        switch (message.getType()) {
            case ALERT:
            case CONFIRM:
            case WARNING:
                log.warn(message.getContent());
                actions.forEach(action -> log.warn("\t* " + action.toString(this)));
                return true;
            case ERROR:
                log.error(message.getContent(), ((Throwable) message.getPayload()));
                actions.forEach(action -> log.error("\t* " + action.toString(this)));
                return true;
            case DEBUG:
                System.out.println(message.getContent());
                actions.forEach(action -> log.debug("\t* " + action.toString(this)));
                return true;
            case INFO:
            case SUCCESS:
            default:
                log.info(message.getContent());
                actions.forEach(action -> log.info("\t* " + action.toString(this)));
                return true;
        }
    }

    @Override
    public String decorateValue(@Nonnull Object p, @Nullable IAzureMessage message) {
        return TextUtils.cyan(p.toString());
    }

    public static class Provider implements AzureMessagerProvider {
        @Nonnull
        public MavenAzureMessager getMessager() {
            return new MavenAzureMessager();
        }
    }
}
