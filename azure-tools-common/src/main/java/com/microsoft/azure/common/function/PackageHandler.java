/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.function.bindings.BindingEnum;
import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;
import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import com.microsoft.azure.maven.function.handlers.CommandHandler;
import com.microsoft.azure.maven.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandler;
import com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandlerImpl;

public class PackageHandler {
	private static final String SEARCH_FUNCTIONS = "Step 1 of 7: Searching for Azure Functions entry points";
	private static final String FOUND_FUNCTIONS = " Azure Functions entry point(s) found.";
	private static final String NO_FUNCTIONS = "Azure Functions entry point not found, plugin will exit.";
	private static final String GENERATE_CONFIG = "Step 2 of 7: Generating Azure Functions configurations";
	private static final String GENERATE_SKIP = "No Azure Functions found. Skip configuration generation.";
	private static final String GENERATE_DONE = "Generation done.";
	private static final String VALIDATE_CONFIG = "Step 3 of 7: Validating generated configurations";
	private static final String VALIDATE_SKIP = "No configurations found. Skip validation.";
	private static final String VALIDATE_DONE = "Validation done.";
	private static final String SAVE_HOST_JSON = "Step 4 of 7: Saving empty host.json";
	private static final String SAVE_FUNCTION_JSONS = "Step 5 of 7: Saving configurations to function.json";
	private static final String SAVE_SKIP = "No configurations found. Skip save.";
	private static final String SAVE_FUNCTION_JSON = "Starting processing function: ";
	private static final String SAVE_SUCCESS = "Successfully saved to ";
	private static final String COPY_JARS = "Step 6 of 7: Copying JARs to staging directory";
	private static final String COPY_SUCCESS = "Copied successfully.";
	private static final String INSTALL_EXTENSIONS = "Step 7 of 7: Installing function extensions if needed";
	private static final String SKIP_INSTALL_EXTENSIONS_HTTP = "Skip install Function extension for HTTP Trigger Functions";
	private static final String INSTALL_EXTENSIONS_FINISH = "Function extension installation done.";
	private static final String BUILD_SUCCESS = "Successfully built Azure Functions.";

	private static final String FUNCTION_JSON = "function.json";
	private static final String HOST_JSON = "host.json";
	public static final String LOCAL_SETTINGS_JSON = "local.settings.json";
	private static final String EXTENSION_BUNDLE = "extensionBundle";

	private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = { BindingEnum.HttpOutput,
			BindingEnum.HttpTrigger };
	private static final String EXTENSION_BUNDLE_ID = "Microsoft.Azure.Functions.ExtensionBundle";
	private static final String SKIP_INSTALL_EXTENSIONS_BUNDLE = "Extension bundle specified, skip install extension";

	public static final String STAGE_DIR_FOUND = "Azure Function App's staging directory found at: ";

	private IFunctionContext ctx;

	public PackageHandler(IFunctionContext ctx) {
		this.ctx = ctx;
	}


	private boolean checkStageDirectoryDirty() throws AzureExecutionException {
		final File stagingDirectory = new File(ctx.getDeploymentStagingDirectoryPath());
		File jarFileInStaging = new File(stagingDirectory, this.ctx.getProject().getJarArtifact().getFileName().toString());
		File jarFileInBuild = this.ctx.getProject().getJarArtifact().toFile();
		if (!jarFileInBuild.exists()) {
			throw new AzureExecutionException("Artifact file '" + jarFileInBuild + "' cannot be found, please run 'gradle jar' to build the project.");
		}

		if (jarFileInBuild.length() == 0) {
			throw new AzureExecutionException("Artifact file '" + jarFileInBuild + "' is empty, please run 'gradle jar' to build the project.");
		}

		if (!stagingDirectory.exists()) {
			Log.info("Stage directory not found, build it now...");
			return true;
		}
		if (stagingDirectory.exists() && stagingDirectory.isFile()) {
			throw new AzureExecutionException("File name conflict: please delete file '" + stagingDirectory.getAbsolutePath() + "', and try again.");
		}
		if (jarFileInStaging.exists() && jarFileInStaging.length() == jarFileInBuild.length()) {
			Log.info(STAGE_DIR_FOUND + ctx.getDeploymentStagingDirectoryPath());
			return false;
		}
		if (jarFileInStaging.exists()) {
			Log.info("Stage directory is dirty, build it now...");
		} else {
			Log.info("Artifact file cannot be found in staging directory, build it now...");
		}
		return true;
	}

	public void execute() throws AzureExecutionException, IOException {
		if (!checkStageDirectoryDirty()) {
			return;
		}
		final AnnotationHandler annotationHandler = getAnnotationHandler();
		final Set<Method> methods = findAnnotatedMethods(annotationHandler);

		if (methods.size() == 0) {
			Log.info(NO_FUNCTIONS);
			return;
		}

		final Map<String, FunctionConfiguration> configMap = getFunctionConfigurations(annotationHandler, methods);

		validateFunctionConfigurations(configMap);

		final ObjectWriter objectWriter = getObjectWriter();

		copyHostJsonFile(objectWriter);

		writeFunctionJsonFiles(objectWriter, configMap);

		copyJarsToStageDirectory();

		final CommandHandler commandHandler = new CommandHandlerImpl();
		final FunctionCoreToolsHandler functionCoreToolsHandler = getFunctionCoreToolsHandler(commandHandler);
		final Set<BindingEnum> bindingClasses = this.getFunctionBindingEnums(configMap);

		installExtension(functionCoreToolsHandler, bindingClasses);

		Log.info(BUILD_SUCCESS);
	}

	// endregion

	// region Process annotations

	private AnnotationHandler getAnnotationHandler() {
		return new AnnotationHandlerImpl();
	}

	private Set<Method> findAnnotatedMethods(final AnnotationHandler handler) throws MalformedURLException {
		Log.info("");
		Log.info(SEARCH_FUNCTIONS);
		Set<Method> functions;
		try {
			Log.debug("ClassPath to resolve: " + getJarArtifactUrl());
			final List<URL> dependencyWithTargetClass = getDependencyArtifactUrls();
			dependencyWithTargetClass.add(getJarArtifactUrl());
			functions = handler.findFunctions(dependencyWithTargetClass);
		} catch (NoClassDefFoundError e) {
			// fallback to reflect through artifact url, for shaded project(fat jar)
			Log.debug("ClassPath to resolve: " + getArtifactUrl());
			functions = handler.findFunctions(Arrays.asList(getArtifactUrl()));
		}
		Log.info(functions.size() + FOUND_FUNCTIONS);
		return functions;
	}

	private URL getArtifactUrl() throws MalformedURLException {
		return this.ctx.getProject().getJarArtifact().toFile().toURI().toURL();
	}

	private URL getJarArtifactUrl() throws MalformedURLException {
		return ctx.getProject().getJarArtifact().toFile().toURI().toURL();
	}

	/**
	 * @return URLs for the classpath with compile scope needed jars
	 */
	private List<URL> getDependencyArtifactUrls() {
		final List<URL> urlList = new ArrayList<>();
		for (final Path jarFilePath : ctx.getProject().getProjectDepencencies()) {
			final File f = jarFilePath.toFile();
			try {
				urlList.add(f.toURI().toURL());
			} catch (MalformedURLException e) {
				Log.debug("Failed to get URL for file: " + f.toString());
			}
		}
		return urlList;
	}

	// endregion

	// region Generate function configurations

	private Map<String, FunctionConfiguration> getFunctionConfigurations(final AnnotationHandler handler,
			final Set<Method> methods) throws AzureExecutionException {
		Log.info("");
		Log.info(GENERATE_CONFIG);
		final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(methods);
		if (configMap.size() == 0) {
			Log.info(GENERATE_SKIP);
		} else {
			final String scriptFilePath = getScriptFilePath();
			configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
			Log.info(GENERATE_DONE);
		}

		return configMap;
	}

	private String getScriptFilePath() {
		return new StringBuilder().append("..").append("/").append(ctx.getProject().getJarArtifact().getFileName().toString())
				.toString();
	}

	// endregion

	// region Validate function configurations

	private void validateFunctionConfigurations(final Map<String, FunctionConfiguration> configMap) {
		Log.info("");
		Log.info(VALIDATE_CONFIG);
		if (configMap.size() == 0) {
			Log.info(VALIDATE_SKIP);
		} else {
			configMap.values().forEach(config -> config.validate());
			Log.info(VALIDATE_DONE);
		}
	}

	// endregion

	// region Write configurations (host.json, function.json) to file

	private void writeFunctionJsonFiles(final ObjectWriter objectWriter,
			final Map<String, FunctionConfiguration> configMap) throws IOException {
		Log.info("");
		Log.info(SAVE_FUNCTION_JSONS);
		if (configMap.size() == 0) {
			Log.info(SAVE_SKIP);
		} else {
			for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
				writeFunctionJsonFile(objectWriter, config.getKey(), config.getValue());
			}
		}
	}

	private void writeFunctionJsonFile(final ObjectWriter objectWriter, final String functionName,
			final FunctionConfiguration config) throws IOException {
		Log.info(SAVE_FUNCTION_JSON + functionName);
		final File functionJsonFile = Paths.get(ctx.getDeploymentStagingDirectoryPath(), functionName, FUNCTION_JSON)
				.toFile();
		writeObjectToFile(objectWriter, config, functionJsonFile);
		Log.info(SAVE_SUCCESS + functionJsonFile.getAbsolutePath());
	}

	protected void copyHostJsonFile(final ObjectWriter objectWriter) throws IOException {
		Log.info("");
		Log.info(SAVE_HOST_JSON);
		final File hostJsonFile = Paths.get(this.ctx.getDeploymentStagingDirectoryPath(), HOST_JSON).toFile();
		FileUtils.copyFile(new File(ctx.getProject().getBaseDirectory().toFile(), HOST_JSON), hostJsonFile);
		Log.info(SAVE_SUCCESS + hostJsonFile.getAbsolutePath());

		final File localSettingJsonFile = Paths.get(this.ctx.getDeploymentStagingDirectoryPath(), LOCAL_SETTINGS_JSON)
				.toFile();
		FileUtils.copyFile(new File(ctx.getProject().getBaseDirectory().toFile(), LOCAL_SETTINGS_JSON), localSettingJsonFile);

		Log.info(SAVE_SUCCESS + localSettingJsonFile.getAbsolutePath());
	}

	private void writeObjectToFile(final ObjectWriter objectWriter, final Object object, final File targetFile)
			throws IOException {
		targetFile.getParentFile().mkdirs();
		targetFile.createNewFile();
		objectWriter.writeValue(targetFile, object);
	}

	private ObjectWriter getObjectWriter() {
		final DefaultPrettyPrinter.Indenter indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE.withLinefeed("\n");
		final PrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(indenter);
		return new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).writer(prettyPrinter);
	}

	// endregion

	// region Copy Jars to stage directory

	private void copyJarsToStageDirectory() throws IOException {
		final String stagingDirectory = ctx.getDeploymentStagingDirectoryPath();
		Log.info("");
		Log.info(COPY_JARS + stagingDirectory);

		for (Path jarFilePath : ctx.getProject().getProjectDepencencies()) {
			if (!jarFilePath.getFileName().startsWith("azure-functions-java-library-")) {
				FileUtils.copyFileToDirectory(jarFilePath.toFile(), new File(ctx.getDeploymentStagingDirectoryPath(), "lib"));
			}
		}

		FileUtils.copyFileToDirectory(ctx.getProject().getJarArtifact().toFile(), new File(ctx.getDeploymentStagingDirectoryPath()));
		Log.info(COPY_SUCCESS);
	}

	// endregion

	// region Azure Functions Core Tools task

	private FunctionCoreToolsHandler getFunctionCoreToolsHandler(final CommandHandler commandHandler) {
		return new FunctionCoreToolsHandlerImpl(commandHandler);
	}

	private void installExtension(final FunctionCoreToolsHandler handler, Set<BindingEnum> bindingEnums)
			throws AzureExecutionException {
		Log.info(INSTALL_EXTENSIONS);
		if (!isInstallingExtensionNeeded(bindingEnums)) {
			return;
		}
		handler.installExtension(new File(this.ctx.getDeploymentStagingDirectoryPath()),
				ctx.getProject().getBaseDirectory().toFile());
		Log.info(INSTALL_EXTENSIONS_FINISH);
	}

	private Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
		final Set<BindingEnum> result = new HashSet<>();
		configMap.values().forEach(
				configuration -> configuration.getBindings().forEach(binding -> result.add(binding.getBindingEnum())));
		return result;
	}

	private boolean isInstallingExtensionNeeded(Set<BindingEnum> bindingTypes) {
		final JsonObject hostJson = readHostJson();
		final JsonObject extensionBundle = hostJson == null ? null : hostJson.getAsJsonObject(EXTENSION_BUNDLE);
		if (extensionBundle != null && extensionBundle.has("id")
				&& StringUtils.equalsIgnoreCase(extensionBundle.get("id").getAsString(), EXTENSION_BUNDLE_ID)) {
			Log.info(SKIP_INSTALL_EXTENSIONS_BUNDLE);
			return false;
		}
		final boolean isNonHttpTriggersExist = bindingTypes.stream()
				.anyMatch(binding -> !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
		if (!isNonHttpTriggersExist) {
			Log.info(SKIP_INSTALL_EXTENSIONS_HTTP);
			return false;
		}
		return true;
	}

	private JsonObject readHostJson() {
		final JsonParser parser = new JsonParser();
		final File hostJson = new File(ctx.getProject().getBaseDirectory().toFile(), HOST_JSON);
		try (final FileInputStream fis = new FileInputStream(hostJson);
				final Scanner scanner = new Scanner(new BOMInputStream(fis))) {
			final String jsonRaw = scanner.useDelimiter("\\Z").next();
			return parser.parse(jsonRaw).getAsJsonObject();
		} catch (IOException e) {
			return null;
		}
	}
	// end region
}
