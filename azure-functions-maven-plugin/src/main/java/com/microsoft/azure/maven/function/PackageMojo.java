/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.function.bindings.BindingEnum;
import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;
import com.microsoft.azure.maven.function.handlers.AnnotationHandler;
import com.microsoft.azure.maven.function.handlers.AnnotationHandlerImpl;
import com.microsoft.azure.maven.function.handlers.CommandHandler;
import com.microsoft.azure.maven.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandler;
import com.microsoft.azure.maven.function.handlers.FunctionCoreToolsHandlerImpl;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generate configuration files (host.json, function.json etc.) and copy JARs to staging directory.
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class PackageMojo extends AbstractFunctionMojo {
    public static final String SEARCH_FUNCTIONS = "Step 1 of 7: Searching for Azure Functions entry points";
    public static final String FOUND_FUNCTIONS = " Azure Functions entry point(s) found.";
    public static final String GENERATE_CONFIG = "Step 2 of 7: Generating Azure Functions configurations";
    public static final String GENERATE_SKIP = "No Azure Functions found. Skip configuration generation.";
    public static final String GENERATE_DONE = "Generation done.";
    public static final String VALIDATE_CONFIG = "Step 3 of 7: Validating generated configurations";
    public static final String VALIDATE_SKIP = "No configurations found. Skip validation.";
    public static final String VALIDATE_DONE = "Validation done.";
    public static final String SAVE_HOST_JSON = "Step 4 of 7: Saving empty host.json";
    public static final String SAVE_FUNCTION_JSONS = "Step 5 of 7: Saving configurations to function.json";
    public static final String SAVE_SKIP = "No configurations found. Skip save.";
    public static final String SAVE_FUNCTION_JSON = "Starting processing function: ";
    public static final String SAVE_SUCCESS = "Successfully saved to ";
    public static final String COPY_JARS = "Step 6 of 7: Copying JARs to staging directory";
    public static final String COPY_SUCCESS = "Copied successfully.";
    public static final String INSTALL_EXTENSIONS = "Step 7 of 7: Installing function extensions if needed";
    public static final String SKIP_INSTALL_EXTENSIONS = "Skip install Function extension for HTTP Trigger Functions";
    public static final String INSTALL_EXTENSIONS_FINISH = "Function extension installation done.";
    public static final String BUILD_SUCCESS = "Successfully built Azure Functions.";

    public static final String FUNCTION_JSON = "function.json";
    public static final String HOST_JSON = "host.json";

    private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION =
        {BindingEnum.HTTP_OUTPUT, BindingEnum.HTTP_TRIGGER};
    //region Entry Point

    @Override
    protected void doExecute() throws Exception {
        final AnnotationHandler annotationHandler = getAnnotationHandler();

        final Set<Method> methods = findAnnotatedMethods(annotationHandler);

        final Map<String, FunctionConfiguration> configMap = getFunctionConfigurations(annotationHandler, methods);

        validateFunctionConfigurations(configMap);

        final ObjectWriter objectWriter = getObjectWriter();

        writeEmptyHostJsonFile(objectWriter);

        writeFunctionJsonFiles(objectWriter, configMap);

        copyJarsToStageDirectory();

        final CommandHandler commandHandler = new CommandHandlerImpl(this.getLog());
        final FunctionCoreToolsHandler functionCoreToolsHandler = getFunctionCoreToolsHandler(commandHandler);
        final Set<BindingEnum> bindingClasses = this.getFunctionBindingEnums(configMap);

        installExtension(functionCoreToolsHandler, bindingClasses);

        info(BUILD_SUCCESS);
    }

    //endregion

    //region Process annotations

    protected AnnotationHandler getAnnotationHandler() {
        return new AnnotationHandlerImpl(getLog());
    }

    protected Set<Method> findAnnotatedMethods(final AnnotationHandler handler) throws Exception {
        info("");
        info(SEARCH_FUNCTIONS);
        Set<Method> functions;
        try {
            debug("ClassPath to resolve: " + getTargetClassUrl());
            final List<URL> dependencyWithTargetClass = getDependencyArtifactUrls();
            dependencyWithTargetClass.add(getTargetClassUrl());
            functions = handler.findFunctions(dependencyWithTargetClass);
        } catch (NoClassDefFoundError e) {
            // fallback to reflect through artifact url, for shaded project(fat jar)
            debug("ClassPath to resolve: " + getArtifactUrl());
            functions = handler.findFunctions(Arrays.asList(getArtifactUrl()));
        }
        info(functions.size() + FOUND_FUNCTIONS);
        return functions;
    }

    protected URL getArtifactUrl() throws Exception {
        return this.getProject().getArtifact().getFile().toURI().toURL();
    }

    protected URL getTargetClassUrl() throws Exception {
        return outputDirectory.toURI().toURL();
    }

    /**
     * @return URLs for the classpath with compile scope needed jars
     */
    protected List<URL> getDependencyArtifactUrls() {
        final List<URL> urlList = new ArrayList<>();
        final List<String> compileClasspathElements = new ArrayList<>();
        try {
            compileClasspathElements.addAll(this.getProject().getCompileClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            debug("Failed to resolve dependencies for compile scope, exception: " + e.getMessage());
        }
        for (final String element : compileClasspathElements) {
            final File f = new File(element);
            try {
                urlList.add(f.toURI().toURL());
            } catch (MalformedURLException e) {
                debug("Failed to get URL for file: " + f.toString());
            }
        }
        return urlList;
    }

    //endregion

    //region Generate function configurations

    protected Map<String, FunctionConfiguration> getFunctionConfigurations(final AnnotationHandler handler,
                                                                           final Set<Method> methods) throws Exception {
        info("");
        info(GENERATE_CONFIG);
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(methods);
        if (configMap.size() == 0) {
            info(GENERATE_SKIP);
        } else {
            final String scriptFilePath = getScriptFilePath();
            configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
            info(GENERATE_DONE);
        }

        return configMap;
    }

    protected String getScriptFilePath() {
        return new StringBuilder()
                .append("..")
                .append(File.separator)
                .append(getFinalName())
                .append(".jar")
                .toString();
    }

    //endregion

    //region Validate function configurations

    protected void validateFunctionConfigurations(final Map<String, FunctionConfiguration> configMap) {
        info("");
        info(VALIDATE_CONFIG);
        if (configMap.size() == 0) {
            info(VALIDATE_SKIP);
        } else {
            configMap.values().forEach(config -> config.validate());
            info(VALIDATE_DONE);
        }
    }

    //endregion

    //region Write configurations (host.json, function.json) to file

    protected void writeFunctionJsonFiles(final ObjectWriter objectWriter,
                                          final Map<String, FunctionConfiguration> configMap) throws IOException {
        info("");
        info(SAVE_FUNCTION_JSONS);
        if (configMap.size() == 0) {
            info(SAVE_SKIP);
        } else {
            for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
                writeFunctionJsonFile(objectWriter, config.getKey(), config.getValue());
            }
        }
    }

    protected void writeFunctionJsonFile(final ObjectWriter objectWriter, final String functionName,
                                         final FunctionConfiguration config) throws IOException {
        info(SAVE_FUNCTION_JSON + functionName);
        final File functionJsonFile = Paths.get(getDeploymentStagingDirectoryPath(),
                functionName, FUNCTION_JSON).toFile();
        writeObjectToFile(objectWriter, config, functionJsonFile);
        info(SAVE_SUCCESS + functionJsonFile.getAbsolutePath());
    }

    protected void writeEmptyHostJsonFile(final ObjectWriter objectWriter) throws IOException {
        info("");
        info(SAVE_HOST_JSON);
        final File hostJsonFile = Paths.get(getDeploymentStagingDirectoryPath(), HOST_JSON).toFile();
        writeObjectToFile(objectWriter, new Object(), hostJsonFile);
        info(SAVE_SUCCESS + hostJsonFile.getAbsolutePath());
    }

    protected void writeObjectToFile(final ObjectWriter objectWriter, final Object object, final File targetFile)
            throws IOException {
        targetFile.getParentFile().mkdirs();
        targetFile.createNewFile();
        objectWriter.writeValue(targetFile, object);
    }

    protected ObjectWriter getObjectWriter() {
        return new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .writerWithDefaultPrettyPrinter();
    }

    //endregion

    //region Copy Jars to stage directory

    protected void copyJarsToStageDirectory() throws IOException {
        final String stagingDirectory = getDeploymentStagingDirectoryPath();
        info("");
        info(COPY_JARS + stagingDirectory);
        Utils.copyResources(
                getProject(),
                getSession(),
                getMavenResourcesFiltering(),
                getResources(),
                stagingDirectory);
        info(COPY_SUCCESS);
    }

    @Override
    public List<Resource> getResources() {
        final Resource resource = new Resource();
        resource.setDirectory(getBuildDirectoryAbsolutePath());
        resource.setTargetPath("/");
        resource.setFiltering(false);
        resource.setIncludes(Arrays.asList("*.jar"));
        return Arrays.asList(resource);
    }

    //endregion

    //region Azure Functions Core Tools task

    protected FunctionCoreToolsHandler getFunctionCoreToolsHandler(final CommandHandler commandHandler) {
        return new FunctionCoreToolsHandlerImpl(this, commandHandler);
    }

    protected void installExtension(final FunctionCoreToolsHandler handler,
                                    Set<BindingEnum> bindingEnums) throws Exception {
        if (!isInstallingExtensionNeeded(bindingEnums)) {
            info(SKIP_INSTALL_EXTENSIONS);
            return;
        }
        info(INSTALL_EXTENSIONS);
        handler.installExtension();
        info(INSTALL_EXTENSIONS_FINISH);
    }

    protected Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
        final Set<BindingEnum> result = new HashSet<>();
        configMap.values().forEach(configuration -> configuration.getBindings().
                forEach(binding -> result.add(binding.getBindingEnum())));
        return result;
    }

    protected boolean isInstallingExtensionNeeded(Set<BindingEnum> bindingTypes) {
        return bindingTypes.stream().anyMatch(binding ->
                !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
    }
    // end region
}
