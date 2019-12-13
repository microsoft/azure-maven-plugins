/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.logging;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

public class DefaultLogger implements ILogger {
    Logger logger = LoggerFactory.getLogger(DefaultLogger.class);

    @Override
    public void debug(String content) {
        debug(content, null);
    }

    @Override
    public void debug(String content, Throwable error) {
        print(System.out, content, error);
    }

    @Override
    public void debug(Throwable error) {
        debug("", error);
    }

    @Override
    public void info(String content) {
        info(content, null);
    }

    @Override
    public void info(Throwable error) {
        info("", error);

    }

    @Override
    public void info(String content, Throwable error) {
        print(System.out, content, error);
    }

    @Override
    public void warn(String content) {
        warn(content, null);
    }

    @Override
    public void warn(String content, Throwable error) {
        print(System.err, content, error);
    }

    @Override
    public void warn(Throwable error) {
        warn("", error);
    }

    @Override
    public void error(String content) {
        error(content, null);
    }

    @Override
    public void error(String content, Throwable error) {
        print(System.err, content, error);
    }

    @Override
    public void error(Throwable error) {
        error("", error);
    }

    private void print(PrintStream output, String content, Throwable error) {
        if (StringUtils.isNotBlank(content)) {
            output.println(content);
        }

        if (error != null) {
            error.printStackTrace(output);
        }
    }

}
