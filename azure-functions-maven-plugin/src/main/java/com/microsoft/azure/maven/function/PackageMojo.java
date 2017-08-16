/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;
import com.microsoft.azure.maven.function.configurations.HostConfiguration;
import com.microsoft.azure.maven.function.configurations.LocalSettings;
import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generate configuration files (host.json, function.json etc.) and copy JARs to staging directory
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractFunctionMojo {
    public static final String SEARCH_FUNCTIONS = "Step 1 of 7: Searching for Azure Function entry points";
    public static final String FOUND_FUNCTIONS = " Azure Function entry point(s) found.";
    public static final String GENERATE_CONFIG = "Step 2 of 7: Generating Azure Function configurations";
    public static final String GENERATE_SKIP = "No Azure Functions found. Skip configuration generation.";
    public static final String GENERATE_DONE = "Generation done.";
    public static final String VALIDATE_CONFIG = "Step 3 of 7: Validating generated configurations";
    public static final String VALIDATE_SKIP = "No configurations found. Skip validation.";
    public static final String VALIDATE_DONE = "Validation done.";
    public static final String SAVE_HOST_JSON = "Step 4 of 7: Saving host configuration to host.json";
    public static final String SAVE_LOCAL_SETTINGS_JSON = "Step 5 of 7: Saving local settings to local.settings.json";
    public static final String SAVE_FUNCTION_JSONS = "Step 6 of 7: Saving configurations to function.json";
    public static final String SAVE_SKIP = "No configurations found. Skip save.";
    public static final String SAVE_FUNCTION_JSON = "Starting processing function: ";
    public static final String SAVE_SUCCESS = "Successfully saved to ";
    public static final String COPY_JARS = "Step 7 of 7: Copying JARs to staging directory ";
    public static final String COPY_SUCCESS = "Copied successfully.";
    public static final String BUILD_SUCCESS = "Successfully built Azure Functions.";

    public static final String FUNCTION_JSON = "function.json";
    public static final String HOST_JSON = "host.json";
    public static final String LOCAL_SETTINGS_JSON = "local.settings.json";

    /**
     * Configuration for host, which will be saved to host.json.
     * Schema of host.json is at https://github.com/Azure/azure-webjobs-sdk-script/blob/dev/schemas/json/host.json
     * Sample host.json is at https://github.com/Azure/azure-webjobs-sdk-script/wiki/host.json
     *
     * @since 0.1.0
     */
    @Parameter
    protected HostConfiguration host;

    public HostConfiguration getHostConfiguration() {
        return host == null ? new HostConfiguration() : host;
    }

    @Override
    protected void doExecute() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();

        final Set<Method> methods = findAnnotatedMethods(handler);

        final Map<String, FunctionConfiguration> configMap = getFunctionConfigurations(handler, methods);

        validateFunctionConfigurations(configMap);

        final ObjectWriter objectWriter = getObjectWriter();

        writeHostJsonFile(objectWriter, getHostConfiguration());

        writeLocalSettingsJsonFile(objectWriter, getLocalSettings());

        writeFunctionJsonFiles(objectWriter, configMap);

        copyJarsToStageDirectory();

        getLog().info(BUILD_SUCCESS);
    }

    protected AnnotationHandler getAnnotationHandler() throws Exception {
        return new AnnotationHandlerImpl(getLog());
    }

    protected Set<Method> findAnnotatedMethods(final AnnotationHandler handler) throws Exception {
        getLog().info(SEARCH_FUNCTIONS);
        final Set<Method> functions = handler.findFunctions(getClassUrl());
        getLog().info(functions.size() + FOUND_FUNCTIONS);
        return functions;
    }

    protected URL getClassUrl() throws Exception {
        return outputDirectory.toURI().toURL();
    }

    protected Map<String, FunctionConfiguration> getFunctionConfigurations(final AnnotationHandler handler,
                                                                           final Set<Method> methods) throws Exception {
        getLog().info(GENERATE_CONFIG);
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(methods);
        if (configMap.size() == 0) {
            getLog().info(GENERATE_SKIP);
        } else {
            final String scriptFilePath = getScriptFilePath();
            configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
            getLog().info(GENERATE_DONE);
        }

        return configMap;
    }

    protected String getScriptFilePath() {
        return new StringBuilder()
                .append("..\\")
                .append(getFinalName())
                .append(".jar")
                .toString();
    }

    protected void validateFunctionConfigurations(final Map<String, FunctionConfiguration> configMap) {
        getLog().info(VALIDATE_CONFIG);
        if (configMap.size() == 0) {
            getLog().info(VALIDATE_SKIP);
        } else {
            configMap.values().forEach(config -> config.validate());
            getLog().info(VALIDATE_DONE);
        }
    }

    protected LocalSettings getLocalSettings() {
        final LocalSettings localSettings = new LocalSettings();
        // Merge <appSettings> to local settings
        final Map appSettings = getAppSettings();
        if (appSettings != null && !appSettings.isEmpty()) {
            localSettings.getValues().putAll(appSettings);
        }
        return localSettings;
    }

    protected void writeFunctionJsonFiles(final ObjectWriter objectWriter,
                                          final Map<String, FunctionConfiguration> configMap) throws IOException {
        getLog().info(SAVE_FUNCTION_JSONS);
        if (configMap.size() == 0) {
            getLog().info(SAVE_SKIP);
        } else {
            for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
                writeFunctionJsonFile(objectWriter, config.getKey(), config.getValue());
            }
        }
    }

    protected void writeFunctionJsonFile(final ObjectWriter objectWriter, final String functionName,
                                         final FunctionConfiguration config) throws IOException {
        getLog().info(SAVE_FUNCTION_JSON + functionName);
        final File functionJsonFile = Paths.get(getDeploymentStageDirectory(), functionName, FUNCTION_JSON).toFile();
        writeObjectToFile(objectWriter, config, functionJsonFile);
        getLog().info(SAVE_SUCCESS + functionJsonFile.getAbsolutePath());
    }

    protected void writeHostJsonFile(final ObjectWriter objectWriter, final HostConfiguration config)
            throws IOException {
        getLog().info(SAVE_HOST_JSON);
        final File hostJsonFile = Paths.get(getDeploymentStageDirectory(), HOST_JSON).toFile();
        writeObjectToFile(objectWriter, config, hostJsonFile);
        getLog().info(SAVE_SUCCESS + hostJsonFile.getAbsolutePath());
    }

    protected void writeLocalSettingsJsonFile(final ObjectWriter objectWriter, final LocalSettings settings)
            throws IOException {
        getLog().info(SAVE_LOCAL_SETTINGS_JSON);
        final File localSettingsFile = Paths.get(getDeploymentStageDirectory(), LOCAL_SETTINGS_JSON).toFile();
        writeObjectToFile(objectWriter, settings, localSettingsFile);
        getLog().info(SAVE_SUCCESS + localSettingsFile.getAbsolutePath());
    }

    protected void writeObjectToFile(final ObjectWriter objectWriter, final Object object, final File targetFile)
            throws IOException {
        targetFile.getParentFile().mkdirs();
        targetFile.createNewFile();
        objectWriter.writeValue(targetFile, object);
    }

    protected ObjectWriter getObjectWriter() {
        return new ObjectMapper().writerWithDefaultPrettyPrinter();
    }

    protected void copyJarsToStageDirectory() throws IOException {
        final String stagingDirectory = getDeploymentStageDirectory();
        getLog().info(COPY_JARS + stagingDirectory);
        Utils.copyResources(
                getProject(),
                getSession(),
                getMavenResourcesFiltering(),
                getResources(),
                stagingDirectory);
        getLog().info(COPY_SUCCESS);
    }

    protected List<Resource> getResources() {
        final Resource resource = new Resource();
        resource.setDirectory(getBuildDirectoryAbsolutePath());
        resource.setTargetPath("/");
        resource.setFiltering(false);
        resource.setIncludes(Arrays.asList("*.jar"));
        return Arrays.asList(resource);
    }
}
