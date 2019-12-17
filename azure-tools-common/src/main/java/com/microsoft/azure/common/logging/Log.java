/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {
	private static final Logger logger = Logger.getLogger(LogUtils.LOGGER_NAME);

    public static void error(String message) {
    	logger.log(Level.SEVERE, message);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void debug(String message) {
        logger.fine(message);
    }

    public static void warn(String message) {
        logger.warning(message);
    }
}
