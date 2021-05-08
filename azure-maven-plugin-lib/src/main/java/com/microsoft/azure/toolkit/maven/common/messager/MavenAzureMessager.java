/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.maven.common.messager;

import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@Slf4j
public class MavenAzureMessager implements IAzureMessager {
    @Override
    public void success(@Nonnull String message, @Nonnull String title) {
        log.info(message);
    }

    @Override
    public void info(@Nonnull String message, @Nonnull String title) {
        log.info(message);
    }

    @Override
    public void warning(@Nonnull String message, @Nonnull String title) {
        log.warn(message);
    }

    @Override
    public void error(@Nonnull String message, @Nonnull String title) {
        log.error(message);
    }

    @Override
    public void error(@Nonnull Throwable throwable, @Nonnull String title) {
        log.error(title, throwable);
    }

    @Override
    public String value(String val) {
        return TextUtils.cyan(val);
    }
}
