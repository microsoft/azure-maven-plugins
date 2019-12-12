package com.microsoft.azure.logging;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public final class Log {
	 private static Logger logger = LoggerFactory.getLogger(Log.class);

	public static void error(Exception ex, String message, Object... arguments) {
		logger.error(String.format(message, arguments), ex);
	}

	public static void error(String message, Object... arguments) {
		logger.error(String.format(message, arguments));
	}

	public static void info(String message, Object... arguments) {
		logger.info(String.format(message, arguments));
	}

	public static void debug(String message, Object... arguments) {
		logger.debug(String.format(message, arguments));
	}


	public static void debug(Exception ex) {
		ex.printStackTrace();
	}


	public static void warn(String message, Object... arguments) {
		logger.warn(String.format(message, arguments));
	}

}
