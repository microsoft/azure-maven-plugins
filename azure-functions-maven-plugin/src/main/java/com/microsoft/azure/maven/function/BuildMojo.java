/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Goal which searches functions in target/classes directory and generates function.json files.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildMojo extends AbstractFunctionMojo {
    public static final String SEARCH_FUNCTIONS = "Searching for Azure Function entry points...";
    public static final String FOUND_FUNCTIONS = " Azure Function entry point(s) found.";
    public static final String GENERATE_CONFIG = "Generating Azure Function configurations...";
    public static final String GENERATE_DONE = "Generation done.";
    public static final String VALIDATE_CONFIG = "Validating generated configurations...";
    public static final String VALIDATE_DONE = "Validation done.";
    public static final String SAVE_CONFIG = "Saving configurations to function.json...";
    public static final String SAVE_SUCCESS = "Saved successfully.";
    public static final String SAVE_SINGLE_CONFIG = "\tStarting processing function: ";
    public static final String SAVE_SINGLE_SUCCESS = "\tSuccessfully saved to ";
    public static final String FUNCTION_JSON = "function.json";
    public static final String COPY_JARS = "Copying JARs to staging directory ";
    public static final String COPY_SUCCESS = "Copies successfully.";
    public static final String BUILD_SUCCESS = "Successfully built Azure Functions.";

    @Override
    protected void doExecute() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();

        final Set<Method> methods = findAnnotatedMethods(handler);

        final Map<String, FunctionConfiguration> configMap = generateConfigurations(handler, methods);

        validateConfigurations(configMap);

        outputJsonFile(configMap);

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

    protected Map<String, FunctionConfiguration> generateConfigurations(final AnnotationHandler handler,
                                                                        final Set<Method> methods) throws Exception {
        getLog().info(GENERATE_CONFIG);
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(methods);
        final String scriptFilePath = getScriptFilePath();
        configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
        getLog().info(GENERATE_DONE);
        return configMap;
    }

    protected String getScriptFilePath() {
        return new StringBuilder()
                .append("..\\")
                .append(getFinalName())
                .append(".jar")
                .toString();
    }

    protected void validateConfigurations(final Map<String, FunctionConfiguration> configMap) {
        getLog().info(VALIDATE_CONFIG);
        configMap.values().forEach(config -> config.validate());
        getLog().info(VALIDATE_DONE);
    }

    protected void outputJsonFile(final Map<String, FunctionConfiguration> configMap) throws IOException {
        getLog().info(SAVE_CONFIG);
        final ObjectWriter objectWriter = getObjectWriter();
        for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
            getLog().info(SAVE_SINGLE_CONFIG + config.getKey());
            final File file = getFunctionJsonFile(config.getKey());
            objectWriter.writeValue(file, config.getValue());
            getLog().info(SAVE_SINGLE_SUCCESS + file.getAbsolutePath());
        }
        getLog().info(SAVE_SUCCESS);
    }

    protected ObjectWriter getObjectWriter() {
        return new ObjectMapper().writerWithDefaultPrettyPrinter();
    }

    protected File getFunctionJsonFile(final String functionName) throws IOException {
        final Path functionDirPath = Paths.get(getDeploymentStageDirectory(), functionName);
        functionDirPath.toFile().mkdirs();
        final File functionJsonFile = Paths.get(functionDirPath.toString(), FUNCTION_JSON).toFile();
        functionJsonFile.createNewFile();
        return functionJsonFile;
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
