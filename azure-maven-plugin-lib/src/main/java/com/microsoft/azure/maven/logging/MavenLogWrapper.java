/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.logging;

import com.microsoft.azure.common.logging.ILogger;

import org.apache.maven.plugin.logging.Log;

public class MavenLogWrapper implements ILogger {
    private Log mavenLogger;

    public MavenLogWrapper(Log mavenLogger) {
        this.mavenLogger = mavenLogger;
    }

    @Override
    public void debug(String content) {
        mavenLogger.debug(content);
    }

    @Override
    public void debug(String content, Throwable error) {
        mavenLogger.debug(content, error);
    }

    @Override
    public void debug(Throwable error) {
        mavenLogger.debug(error);
    }

    @Override
    public void info(String content) {
        mavenLogger.info(content);
    }

    @Override
    public void info(String content, Throwable error) {
        mavenLogger.info(content, error);
    }

    @Override
    public void info(Throwable error) {
        mavenLogger.info(error);
    }

    @Override
    public void warn(String content) {
        mavenLogger.warn(content);
    }

    @Override
    public void warn(String content, Throwable error) {
        mavenLogger.warn(content, error);
    }

    @Override
    public void warn(Throwable error) {
        mavenLogger.warn(error);
    }

    @Override
    public void error(String content) {
        mavenLogger.error(content);
    }

    @Override
    public void error(String content, Throwable error) {
        mavenLogger.error(content, error);
    }

    @Override
    public void error(Throwable error) {
        mavenLogger.error(error);
    }
}
