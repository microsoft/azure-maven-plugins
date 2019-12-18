package com.microsoft.azure.common.function;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.microsoft.azure.common.logging.Log;


public class ListHandler {
	private static final String TEMPLATES_START = ">> templates begin <<";
	private static final String TEMPLATES_END = ">> templates end <<";

	private static final String BINDINGS_START = ">> bindings begin <<";
	private static final String BINDINGS_END = ">> bindings end <<";

	private static final String RESOURCES_START = ">> resources begin <<";
	private static final String RESOURCES_END = ">> resources end <<";

	private static final String TEMPLATES_FILE = "/templates.json";
	private static final String BINDINGS_FILE = "/bindings.json";
	private static final String RESOURCES_FILE = "/resources.json";

	public void execute() throws Exception {
		Log.info(TEMPLATES_START);
		printToSystemOut(TEMPLATES_FILE);
		Log.info(TEMPLATES_END);

		Log.info(BINDINGS_START);
		printToSystemOut(BINDINGS_FILE);
		Log.info(BINDINGS_END);

		Log.info(RESOURCES_START);
		printToSystemOut(RESOURCES_FILE);
		Log.info(RESOURCES_END);
	}

	private void printToSystemOut(String file) throws IOException {
		try (final InputStream is = ListHandler.class.getResourceAsStream(file)) {
			IOUtils.copy(is, System.out);
		}
	}
}
