/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LogUtils {
	public static final String LOGGER_NAME = "azure-tools";
	private static final Logger logger = Logger.getLogger(LOGGER_NAME);



    /**
     * Initialize logger for logger handler and logger level.
     *
     * @param handler the logger handler.
     * @param level the logger level for logger.
     */
    public static void initialize(Handler handler, Level level) {
        logger.addHandler(handler);
        logger.setLevel(level);
    }
}
