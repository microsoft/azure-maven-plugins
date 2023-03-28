/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.maven.common.logger;

import com.microsoft.azure.toolkit.lib.common.logger.Logger;
import com.microsoft.azure.toolkit.lib.common.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nullable;
import java.util.Objects;

@RequiredArgsConstructor
public class MavenLogger implements Logger {
    private final Log logger;

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isErrorEnabled();
    }

    @Override
    public void debug(String message, @Nullable Throwable t) {
        this.logger.debug(message, t);
    }

    @Override
    public void info(String message, @Nullable Throwable t) {
        this.logger.info(message, t);
    }

    @Override
    public void warn(String message, @Nullable Throwable t) {
        this.logger.warn(message, t);
    }

    @Override
    public void error(String message, @Nullable Throwable t) {
        this.logger.error(message, t);
    }

    @RequiredArgsConstructor
    public static class Factory extends LoggerFactory {
        private final AbstractMojo mojo;
        private MavenLogger logger;

        @Override
        public synchronized Logger getLogger(Class<?> clazz) {
            if (Objects.isNull(logger)) {
                logger = new MavenLogger(mojo.getLog());
            }
            return logger;
        }
    }
}