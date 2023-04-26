/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.AnnotationHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.AnnotationHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandlerImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.shade.DefaultShader;
import org.apache.maven.plugins.shade.ShadeRequest;
import org.apache.maven.plugins.shade.Shader;
import org.apache.maven.plugins.shade.filter.Filter;
import org.apache.maven.plugins.shade.filter.SimpleFilter;
import org.apache.maven.plugins.shade.resource.ApacheLicenseResourceTransformer;
import org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.plugins.shade.resource.ServicesResourceTransformer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generate configuration files (host.json, function.json etc.) and copy JARs to staging directory.
 */
@Slf4j
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageMojo extends AbstractFunctionMojo {
    public static final String SEARCH_FUNCTIONS = "Step 1 of 8: Searching for Azure Functions entry points";
    public static final String FOUND_FUNCTIONS = " Azure Functions entry point(s) found.";
    public static final String NO_FUNCTIONS = "Azure Functions entry point not found, plugin will exit.";
    public static final String GENERATE_CONFIG = "Step 2 of 8: Generating Azure Functions configurations";
    public static final String GENERATE_SKIP = "No Azure Functions found. Skip configuration generation.";
    public static final String GENERATE_DONE = "Generation done.";
    public static final String VALIDATE_CONFIG = "Step 3 of 8: Validating generated configurations";
    public static final String VALIDATE_SKIP = "No configurations found. Skip validation.";
    public static final String VALIDATE_DONE = "Validation done.";
    public static final String SAVING_HOST_JSON = "Step 4 of 8: Copying/creating host.json";
    public static final String SAVING_LOCAL_SETTINGS_JSON = "Step 5 of 8: Copying/creating local.settings.json";
    public static final String SAVE_FUNCTION_JSONS = "Step 6 of 8: Saving configurations to function.json";
    public static final String SAVE_SKIP = "No configurations found. Skip save.";
    public static final String SAVE_FUNCTION_JSON = "Starting processing function: ";
    public static final String SAVE_SUCCESS = "Successfully saved to ";
    public static final String COPY_JARS = "Step 7 of 8: Copying JARs to staging directory ";
    public static final String COPY_SUCCESS = "Copied successfully.";
    public static final String INSTALL_EXTENSIONS = "Step 8 of 8: Installing function extensions if needed";
    public static final String SKIP_INSTALL_EXTENSIONS_HTTP = "Skip install Function extension for HTTP Trigger Functions";
    public static final String INSTALL_EXTENSIONS_FINISH = "Function extension installation done.";
    public static final String BUILD_SUCCESS = "Successfully built Azure Functions.";

    public static final String FUNCTION_JSON = "function.json";
    public static final String EXTENSION_BUNDLE = "extensionBundle";
    private static final String AZURE_FUNCTIONS_JAVA_CORE_LIBRARY = "azure-functions-java-core-library";
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[4.*, 5.0.0)\"}}\n";

    private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = {BindingEnum.HttpOutput, BindingEnum.HttpTrigger};
    private static final String EXTENSION_BUNDLE_ID = "Microsoft.Azure.Functions.ExtensionBundle";
    private static final String EXTENSION_BUNDLE_PREVIEW_ID = "Microsoft.Azure.Functions.ExtensionBundle.Preview";
    private static final String SKIP_INSTALL_EXTENSIONS_FLAG = "skipInstallExtensions flag is set, skip install extension";
    private static final String SKIP_INSTALL_EXTENSIONS_BUNDLE = "Extension bundle specified, skip install extension";
    private static final String BUILD_UBER_ARTIFACT_EXCEPTION = "Failed to build uber artifact, please set `buildFatJar` to `false` and use `maven-shade-plugin` to try again.";
    //region Entry Point

    /**
     * Boolean flag to skip extension installation
     */
    @Parameter(property = "functions.skipInstallExtensions", defaultValue = "false")
    protected Boolean skipInstallExtensions;

    /**
     * Boolean flag to control whether to skip copy dependencies to staging directory
     */
    @Parameter(property = "functions.skipCopyDependencies", defaultValue = "false")
    protected Boolean skipCopyDependencies;

    /**
     * Boolean flag to control whether to build fat jar or use the original jar with dependencies in lib folder
     */
    @Parameter(property = "functions.buildJarWithDependencies", defaultValue = "false")
    protected Boolean buildJarWithDependencies;

    @Override
    @AzureOperation("user/functionapp.package")
    protected void doExecute() throws AzureExecutionException {
        validateAppName();
        validateFunctionCompatibility();
        promptCompileInfo();

        final AnnotationHandler annotationHandler = getAnnotationHandler();

        final Set<Method> methods;
        try {
            methods = findAnnotatedMethods(annotationHandler);
        } catch (MalformedURLException e) {
            throw new AzureExecutionException("Invalid URL when resolving class path:" + e.getMessage(), e);
        }

        if (methods.size() == 0) {
            log.info(NO_FUNCTIONS);
            return;
        }

        final Map<String, FunctionConfiguration> configMap = getFunctionConfigurations(annotationHandler, methods);

        trackFunctionProperties(configMap);
        validateFunctionConfigurations(configMap);

        final ObjectWriter objectWriter = getObjectWriter();

        try {
            copyHostJson();

            copyLocalSettingsJson();

            writeFunctionJsonFiles(objectWriter, configMap);

            copyJarsToStageDirectory();
        } catch (IOException | MojoExecutionException e) {
            throw new AzureExecutionException("Cannot perform IO operations due to error:" + e.getMessage(), e);
        }

        final CommandHandler commandHandler = new CommandHandlerImpl();
        final FunctionCoreToolsHandler functionCoreToolsHandler = getFunctionCoreToolsHandler(commandHandler);
        final Set<BindingEnum> bindingClasses = this.getFunctionBindingEnums(configMap);

        installExtension(functionCoreToolsHandler, bindingClasses);

        log.info(BUILD_SUCCESS);
    }

    public static void buildArtifactWithDependencies(@Nonnull final File artifactFile, @Nullable final Set<File> dependencies, final File target) {
        AzureMessager.getMessager().info("Building artifact with dependencies...");
        final Shader shader = new DefaultShader();
        final ShadeRequest shadeRequest = new ShadeRequest();
        final Set<File> jars = new HashSet<>();
        jars.add(artifactFile);
        Optional.ofNullable(dependencies).ifPresent(jars::addAll);
        shadeRequest.setJars(jars);
        shadeRequest.setRelocators(Collections.emptyList());
        shadeRequest.setFilters(Collections.singletonList(getExcludeSignFilesFilter(jars)));
        shadeRequest.setShadeSourcesContent(false);
        shadeRequest.setUberJar(target);
        shadeRequest.setResourceTransformers(getDefaultResourceTransformers());
        try {
            shader.shade(shadeRequest);
        } catch (IOException | MojoExecutionException e) {
            throw new AzureToolkitRuntimeException(BUILD_UBER_ARTIFACT_EXCEPTION, e);
        }
        AzureMessager.getMessager().info(AzureString.format("Successfully build artifact to %s", target.getAbsolutePath()));
    }

    private static List<ResourceTransformer> getDefaultResourceTransformers() {
        return Arrays.asList(new ServicesResourceTransformer(), new ApacheLicenseResourceTransformer(), new ApacheNoticeResourceTransformer());
    }

    private static Filter getExcludeSignFilesFilter(final Set<File> jars) {
        return new SimpleFilter(jars, Collections.emptySet(), SetUtils.unmodifiableSet("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA"));
    }

    //endregion

    //region Process annotations

    protected AnnotationHandler getAnnotationHandler() {
        return new AnnotationHandlerImpl();
    }

    protected Set<Method> findAnnotatedMethods(final AnnotationHandler handler) throws MalformedURLException {
        log.info("");
        log.info(SEARCH_FUNCTIONS);
        Set<Method> functions;
        try {
            log.debug("ClassPath to resolve: " + getTargetClassUrl());
            final List<URL> dependencyWithTargetClass = getDependencyArtifactUrls();
            dependencyWithTargetClass.add(getTargetClassUrl());
            functions = handler.findFunctions(dependencyWithTargetClass);
        } catch (NoClassDefFoundError e) {
            // fallback to reflect through artifact url, for shaded project(fat jar)
            log.debug("ClassPath to resolve: " + getArtifactUrl());
            functions = handler.findFunctions(Collections.singletonList(getArtifactUrl()));
        }
        log.info(functions.size() + FOUND_FUNCTIONS);
        return functions;
    }

    protected URL getArtifactUrl() throws MalformedURLException {
        return this.getArtifact().toURI().toURL();
    }

    protected URL getTargetClassUrl() throws MalformedURLException {
        return outputDirectory.toURI().toURL();
    }

    /**
     * @return URLs for the classpath with compile scope needed jars
     */
    protected List<URL> getDependencyArtifactUrls() {
        final List<URL> urlList = new ArrayList<>();
        final List<String> runtimeClasspathElements = new ArrayList<>();
        try {
            runtimeClasspathElements.addAll(this.getProject().getRuntimeClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            log.debug("Failed to resolve dependencies for compile scope, exception: " + e.getMessage());
        }
        for (final String element : runtimeClasspathElements) {
            final File f = new File(element);
            try {
                urlList.add(f.toURI().toURL());
            } catch (MalformedURLException e) {
                log.debug("Failed to get URL for file: " + f);
            }
        }
        return urlList;
    }

    //endregion

    //region Generate function configurations

    protected Map<String, FunctionConfiguration> getFunctionConfigurations(final AnnotationHandler handler,
                                                                           final Set<Method> methods) throws AzureExecutionException {
        log.info("");
        log.info(GENERATE_CONFIG);
        final Map<String, FunctionConfiguration> configMap = handler.generateConfigurations(methods);
        if (configMap.size() == 0) {
            log.info(GENERATE_SKIP);
        } else {
            final String scriptFilePath = getScriptFilePath();
            configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
            log.info(GENERATE_DONE);
        }

        return configMap;
    }

    protected String getScriptFilePath() {
        return String.format("../%s.jar", getFinalName());
    }

    //endregion

    //region Validate function configurations

    protected void validateFunctionConfigurations(final Map<String, FunctionConfiguration> configMap) {
        log.info("");
        log.info(VALIDATE_CONFIG);
        if (configMap.size() == 0) {
            log.info(VALIDATE_SKIP);
        } else {
            configMap.values().forEach(FunctionConfiguration::validate);
            log.info(VALIDATE_DONE);
        }
    }

    //endregion

    //region Write configurations (host.json, function.json) to file

    protected void writeFunctionJsonFiles(final ObjectWriter objectWriter,
                                          final Map<String, FunctionConfiguration> configMap) throws IOException {
        log.info("");
        log.info(SAVE_FUNCTION_JSONS);
        if (configMap.size() == 0) {
            log.info(SAVE_SKIP);
        } else {
            for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
                writeFunctionJsonFile(objectWriter, config.getKey(), config.getValue());
            }
        }
    }

    protected void writeFunctionJsonFile(final ObjectWriter objectWriter, final String functionName,
                                         final FunctionConfiguration config) throws IOException {
        log.info(SAVE_FUNCTION_JSON + functionName);
        final File functionJsonFile = Paths.get(getDeploymentStagingDirectoryPath(),
                functionName, FUNCTION_JSON).toFile();
        writeObjectToFile(objectWriter, config, functionJsonFile);
        log.info(SAVE_SUCCESS + functionJsonFile.getAbsolutePath());
    }

    protected void copyHostJson() throws IOException {
        log.info("");
        log.info(SAVING_HOST_JSON);
        final File sourceHostJsonFile = getHostJsonFile();
        final File destHostJsonFile = Paths.get(getDeploymentStagingDirectoryPath(), HOST_JSON).toFile();
        copyFilesWithDefaultContent(sourceHostJsonFile, destHostJsonFile, DEFAULT_HOST_JSON);
        log.info(SAVE_SUCCESS + destHostJsonFile.getAbsolutePath());
    }

    protected void copyLocalSettingsJson() throws IOException {
        log.info("");
        log.info(SAVING_LOCAL_SETTINGS_JSON);
        final File sourceLocalSettingsJsonFile = getLocalSettingsJsonFile();
        final File destLocalSettingsJsonFile = Paths.get(getDeploymentStagingDirectoryPath(), LOCAL_SETTINGS_JSON).toFile();
        copyFilesWithDefaultContent(sourceLocalSettingsJsonFile, destLocalSettingsJsonFile, DEFAULT_LOCAL_SETTINGS_JSON);
        log.info(SAVE_SUCCESS + destLocalSettingsJsonFile.getAbsolutePath());
    }

    private static void copyFilesWithDefaultContent(File source, File dest, String defaultContent)
            throws IOException {
        if (source != null && source.exists()) {
            FileUtils.copyFile(source, dest);
        } else {
            FileUtils.write(dest, defaultContent, Charset.defaultCharset());
        }
    }

    protected void writeObjectToFile(final ObjectWriter objectWriter, final Object object, final File targetFile)
            throws IOException {
        targetFile.getParentFile().mkdirs();
        targetFile.createNewFile();
        objectWriter.writeValue(targetFile, object);
    }

    protected ObjectWriter getObjectWriter() {
        final DefaultPrettyPrinter.Indenter indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE.withLinefeed(StringUtils.LF);
        final PrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(indenter);
        return new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writer(prettyPrinter);
    }

    //endregion

    //region Copy Jars to stage directory

    protected void copyJarsToStageDirectory() throws IOException, MojoExecutionException {
        final File stagingDirectory = new File(getDeploymentStagingDirectoryPath());
        log.info("");
        log.info(COPY_JARS + stagingDirectory.getAbsolutePath());
        final Set<Artifact> artifacts = project.getArtifacts();
        final String libraryToExclude = artifacts.stream()
                .map(Artifact::getArtifactId)
                .filter(artifactId -> StringUtils.equalsAnyIgnoreCase(artifactId, AZURE_FUNCTIONS_JAVA_CORE_LIBRARY)).findFirst().orElse(AZURE_FUNCTIONS_JAVA_LIBRARY);
        final Set<File> dependencies = artifacts.stream().filter(artifact -> !StringUtils.equalsIgnoreCase(artifact.getArtifactId(), libraryToExclude))
                .map(Artifact::getFile).collect(Collectors.toSet());
        copyArtifactToStagingDirectory(stagingDirectory, dependencies);
        copyDependenciesToStagingDirectory(stagingDirectory, dependencies);
        log.info(COPY_SUCCESS);
    }

    private void copyDependenciesToStagingDirectory(@Nonnull final File stagingDirectory, @Nullable final Set<File> dependencies) throws IOException {
        if (skipCopyDependencies) {
            log.info("Skip copy dependencies to staging directory as `skipCopyDependencies` is set to true.");
        } else if (buildJarWithDependencies) {
            log.info("Skip copy dependencies to staging directory as `buildJarWithDependencies` is set to true, dependencies has been included in the artifact.");
        } else {
            final File libFolder = new File(stagingDirectory, "lib");
            if (libFolder.exists()) {
                FileUtils.cleanDirectory(libFolder);
            }
            Optional.ofNullable(dependencies).ifPresent(des -> des.forEach(dependency -> copyFileToDirectory(dependency, libFolder)));
        }
    }

    private void copyArtifactToStagingDirectory(@Nonnull final File stagingDirectory, @Nullable final Set<File> dependencies) throws IOException {
        final File originalArtifact = getArtifact();
        final File finalArtifact = this.buildJarWithDependencies ?
                com.microsoft.azure.toolkit.lib.appservice.utils.Utils.createTempFile(FilenameUtils.getBaseName(originalArtifact.getName()), ".jar") : originalArtifact;
        if (buildJarWithDependencies) {
            buildArtifactWithDependencies(originalArtifact, dependencies, finalArtifact);
        }
        FileUtils.copyFile(finalArtifact, new File(stagingDirectory, originalArtifact.getName()));
    }

    @Override
    public List<DeploymentResource> getResources() {
        final DeploymentResource resource = new DeploymentResource();
        resource.setDirectory(getBuildDirectoryAbsolutePath());
        resource.setTargetPath("/");
        resource.setFiltering(false);
        resource.setIncludes(Collections.singletonList("*.jar"));
        return Collections.singletonList(resource);
    }

    //endregion

    //region Azure Functions Core Tools task

    protected FunctionCoreToolsHandler getFunctionCoreToolsHandler(final CommandHandler commandHandler) {
        return new FunctionCoreToolsHandlerImpl(commandHandler);
    }

    protected void installExtension(final FunctionCoreToolsHandler handler,
                                    Set<BindingEnum> bindingEnums) throws AzureExecutionException {
        log.info(INSTALL_EXTENSIONS);
        if (!isInstallingExtensionNeeded(bindingEnums)) {
            return;
        }
        handler.installExtension(new File(this.getDeploymentStagingDirectoryPath()),
                project.getBasedir());
        log.info(INSTALL_EXTENSIONS_FINISH);
    }

    protected Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
        final Set<BindingEnum> result = new HashSet<>();
        configMap.values().forEach(configuration -> configuration.getBindings().
                forEach(binding -> result.add(binding.getBindingEnum())));
        return result;
    }

    protected boolean isInstallingExtensionNeeded(Set<BindingEnum> bindingTypes) {
        if (BooleanUtils.isTrue(skipInstallExtensions)) {
            log.info(SKIP_INSTALL_EXTENSIONS_FLAG);
            return false;
        }
        final String extensionBundleId = Optional.ofNullable(readHostJson())
                .map(node -> node.at("/extensionBundle/id"))
                .filter(node -> !node.isMissingNode())
                .map(JsonNode::asText)
                .orElse(null);
        if (StringUtils.equalsAnyIgnoreCase(extensionBundleId, EXTENSION_BUNDLE_ID, EXTENSION_BUNDLE_PREVIEW_ID)) {
            log.info(SKIP_INSTALL_EXTENSIONS_BUNDLE);
            return false;
        }
        final boolean isNonHttpTriggersExist = bindingTypes.stream().anyMatch(binding ->
                !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
        if (!isNonHttpTriggersExist) {
            log.info(SKIP_INSTALL_EXTENSIONS_HTTP);
            return false;
        }
        return true;
    }
    // end region

    protected void promptCompileInfo() {
        try {
            log.info(String.format("Java home : %s", System.getenv("JAVA_HOME")));
            log.info(String.format("Artifact compile version : %s", Utils.getArtifactCompileVersion(getArtifact())));
        } catch (AzureToolkitRuntimeException e) {
            // swallow exception when prompt compile info
        }
    }

    protected void trackFunctionProperties(Map<String, FunctionConfiguration> configMap) {
        final List<String> bindingTypeSet = configMap.values().stream().flatMap(configuration -> configuration.getBindings().stream())
                .map(Binding::getType)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        getTelemetryProxy().addDefaultProperty(TRIGGER_TYPE, StringUtils.join(bindingTypeSet, ","));
    }

    private static void copyFileToDirectory(@Nonnull final File srcFile, @Nonnull final File destFile) {
        if (!Objects.equals(srcFile.getParentFile(), destFile)) {
            try {
                FileUtils.copyFileToDirectory(srcFile, destFile);
            } catch (IOException e) {
                throw new AzureToolkitRuntimeException(e);
            }
        }
    }
}
