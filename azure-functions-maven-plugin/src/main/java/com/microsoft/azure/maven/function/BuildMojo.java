/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Goal which searches functions in target/classes directory and generates function.json files.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildMojo extends AbstractFunctionMojo {

    @Override
    protected void doExecute() throws Exception {
        final AnnotationHandler handler = getAnnotationHandler();

        getLog().info("Searching for Azure Function entry points...");
        final Set<Method> functions = handler.findFunctions(getClassUrl());
        getLog().info(functions.size() + " Azure Function entry point(s) found.");

        getLog().info("Generating Azure Function configurations...");
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(functions);
        final String scriptFilePath = getScriptFilePath();
        configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
        getLog().info("Generation done.");

        getLog().info("Validating generated configurations...");
        configMap.values().forEach(config -> config.validate());
        getLog().info("Validation done.");

        getLog().info("Saving configurations to function.json...");
        outputJsonFile(configMap);
        getLog().info("Saved successfully.");

        getLog().info("function.json generation completed successfully.");
    }

    protected AnnotationHandler getAnnotationHandler() throws Exception {
        return new AnnotationHandlerImpl(getLog());
    }

    protected URL getClassUrl() throws Exception {
        return outputDirectory.toURI().toURL();
    }

    protected String getScriptFilePath() {
        return new StringBuilder()
                .append("..\\")
                .append(getFinalName())
                .append(".jar")
                .toString();
    }

    protected void outputJsonFile(final Map<String, FunctionConfiguration> configMap) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
            getLog().info("Starting processing function: " + config.getKey());
            final File file = getFunctionJsonFile(config.getKey());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, config.getValue());
            getLog().info("Successfully write to " + file.getAbsolutePath());
        }
    }

    protected File getFunctionJsonFile(final String functionName) throws IOException {
        final Path functionDirPath = Paths.get(getDeploymentStageDirectory(), functionName);
        functionDirPath.toFile().mkdirs();
        final File functionJsonFile = Paths.get(functionDirPath.toString(), "function.json").toFile();
        functionJsonFile.createNewFile();
        return functionJsonFile;
    }
}
