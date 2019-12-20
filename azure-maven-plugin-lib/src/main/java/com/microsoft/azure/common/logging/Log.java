/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {
    public static final String LOGGER_NAME = "azure-tools";

    private static final Logger logger = Logger.getLogger(LOGGER_NAME);

    public static void error(String message) {
        logger.severe(message);
    }

    public static void error(Exception error) {
        logger.severe(getErrorDetail(error));
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void info(Exception error) {
        logger.info(getErrorDetail(error));
    }

    public static void debug(String message) {
        logger.fine(message);
    }

    public static void debug(Exception error) {
        logger.fine(getErrorDetail(error));
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void warn(Exception error) {
        logger.warning(getErrorDetail(error));
    }

    public static boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    private static String getErrorDetail(Exception error) {
        final StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        final String exceptionDetails = sw.toString();
        try {
            sw.close();
        } catch (IOException e) {
            logger.severe(String.format("Cannot close the StringWriter caused IOException: %s.", e.getMessage()));
        }
        return exceptionDetails;
    }
}
