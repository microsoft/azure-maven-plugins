/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.io.input.BOMInputStream;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public class FunctionStagingInitializer extends AbstractFunctionStagingInitializer {
    private static final String TRIGGER_TYPE = "triggerType";
    protected static final String LINE_FEED = "\r\n";
    protected static final String FUNCTION_JSON = "function.json";
    protected static final String HOST_JSON = "host.json";
    protected static final String LOCAL_SETTINGS_JSON = "local.settings.json";
    protected static final String EXTENSION_BUNDLE = "extensionBundle";
    protected static final String SEARCH_FUNCTIONS = "Step 1 of 8: Searching for Azure Functions entry points";
    protected static final String FOUND_FUNCTIONS = " Azure Functions entry point(s) found.";
    protected static final String NO_FUNCTIONS = "Azure Functions entry point not found, plugin will exit.";
    protected static final String GENERATE_CONFIG = "Step 2 of 8: Generating Azure Functions configurations";
    protected static final String GENERATE_SKIP = "No Azure Functions found. Skip configuration generation.";
    protected static final String GENERATE_DONE = "Generation done.";
    protected static final String VALIDATE_CONFIG = "Step 3 of 8: Validating generated configurations";
    protected static final String VALIDATE_SKIP = "No configurations found. Skip validation.";
    protected static final String VALIDATE_DONE = "Validation done.";
    protected static final String SAVING_HOST_JSON = "Step 4 of 8: Copying/creating host.json";
    protected static final String SAVING_LOCAL_SETTINGS_JSON = "Step 5 of 8: Copying/creating local.settings.json";
    protected static final String SAVE_FUNCTION_JSONS = "Step 6 of 8: Saving configurations to function.json";
    protected static final String SAVE_SKIP = "No configurations found. Skip save.";
    protected static final String SAVE_FUNCTION_JSON = "Starting processing function: ";
    protected static final String SAVE_SUCCESS = "Successfully saved to ";
    protected static final String COPY_JARS = "Step 7 of 8: Copying JARs to staging directory";
    protected static final String COPY_SUCCESS = "Copied successfully.";
    protected static final String INSTALL_EXTENSIONS = "Step 8 of 8: Installing function extensions if needed";
    protected static final String SKIP_INSTALL_EXTENSIONS_HTTP = "Skip install Function extension for HTTP Trigger Functions";
    protected static final String INSTALL_EXTENSIONS_FINISH = "Function extension installation done.";
    protected static final String BUILD_SUCCESS = "Successfully built Azure Functions.";

    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[1.*, 2.0.0)\"}}\n";

    private static final String SKIP_INSTALL_EXTENSIONS_FLAG = "skipInstallExtensions flag is set, skip install extension";
    private static final String SKIP_INSTALL_EXTENSIONS_BUNDLE = "Extension bundle specified, skip install extension";
    private static final String EXTENSION_BUNDLE_ID = "Microsoft.Azure.Functions.ExtensionBundle";
    private static final String EXTENSION_BUNDLE_PREVIEW_ID = "Microsoft.Azure.Functions.ExtensionBundle.Preview";
    private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = { BindingEnum.HttpOutput, BindingEnum.HttpTrigger };

    private Function<FunctionProject, List<FunctionMethod>> methodRetriever;
    private Consumer<FunctionProject> installer;

    @AzureOperation(
            name = "function.prepare_staging_folder",
            type = AzureOperation.Type.TASK
    )
    public void prepareStagingFolder(FunctionProject project, boolean installExtension) {
        final List<FunctionMethod> methods = findAnnotatedMethodsInner(project);

        if (methods.isEmpty()) {
            AzureMessager.getMessager().info(NO_FUNCTIONS);
            return;
        }

        final Map<String, FunctionConfiguration> configMap = generateConfigurations(project, methods);

        trackFunctionProperties(configMap);

        validateFunctionConfigurations(configMap);

        final ObjectWriter objectWriter = getObjectWriter();

        try {
            copyHostJson(project);
            copyLocalSettingsJson(project);
            writeFunctionJsonFiles(project, objectWriter, configMap);
            copyJarsToStageDirectory(project);
            final Set<BindingEnum> bindingEnums = this.getFunctionBindingEnums(configMap);

            if (isInstallingExtensionNeeded(!installExtension, project, bindingEnums)) {
                installExtensionStep(project);
            }
            AzureMessager.getMessager().info(BUILD_SUCCESS);
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException("Cannot perform IO operations due to error:" + e.getMessage(), e);
        }
    }

    @AzureOperation(
            name = "function.list_function_methods",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public List<FunctionMethod> findAnnotatedMethodsInner(FunctionProject project) {
        AzureMessager.getMessager().info(LINE_FEED + SEARCH_FUNCTIONS);
        try {
            final List<FunctionMethod> functions = methodRetriever.apply(project);
            AzureMessager.getMessager().info(functions.size() + FOUND_FUNCTIONS);
            return functions;

        } catch (Exception ex) {
            // Log and warn
            AzureMessager.getMessager().error(ex, "Encounter error when parsing Azure Function annotations.");
            return Collections.emptyList();
        }
    }

    protected Map<String, FunctionConfiguration> generateConfigurations(FunctionProject project, List<FunctionMethod> methods) {
        AzureMessager.getMessager().info(LINE_FEED + GENERATE_CONFIG);
        final Map<String, FunctionConfiguration> configMap = generateConfigurationsInner(project, methods);
        if (configMap.size() == 0) {
            AzureMessager.getMessager().info(GENERATE_SKIP);
        }
        AzureMessager.getMessager().info(GENERATE_DONE);
        return configMap;
    }

    private void installExtensionStep(FunctionProject project) {
        AzureMessager.getMessager().info(INSTALL_EXTENSIONS);
        installer.accept(project);
        AzureMessager.getMessager().info(INSTALL_EXTENSIONS_FINISH);
    }

    private void validateFunctionConfigurations(final Map<String, FunctionConfiguration> configMap) {
        AzureMessager.getMessager().info(LINE_FEED + VALIDATE_CONFIG);
        if (configMap.isEmpty()) {
            AzureMessager.getMessager().info(VALIDATE_SKIP);
        } else {
            configMap.values().forEach(FunctionConfiguration::validate);
            AzureMessager.getMessager().info(VALIDATE_DONE);
        }
    }

    private void writeFunctionJsonFiles(FunctionProject project, final ObjectWriter objectWriter,
                                        final Map<String, FunctionConfiguration> configMap) throws IOException {
        AzureMessager.getMessager().info(LINE_FEED + SAVE_FUNCTION_JSONS);
        if (configMap.size() == 0) {
            AzureMessager.getMessager().info(SAVE_SKIP);
        } else {
            for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
                writeFunctionJsonFile(project, objectWriter, config.getKey(), config.getValue());
            }
        }
    }

    private void writeFunctionJsonFile(FunctionProject project, final ObjectWriter objectWriter, final String functionName,
                                       final FunctionConfiguration config) throws IOException {
        AzureMessager.getMessager().info(SAVE_FUNCTION_JSON + functionName);
        final File functionJsonFile = Paths.get(project.getStagingFolder().getAbsolutePath(),
                functionName, FUNCTION_JSON).toFile();
        writeObjectToFile(objectWriter, config, functionJsonFile);
        AzureMessager.getMessager().info(SAVE_SUCCESS + functionJsonFile.getAbsolutePath());
    }

    private void copyHostJson(FunctionProject project) throws IOException {
        AzureMessager.getMessager().info(LINE_FEED + SAVING_HOST_JSON);
        final File sourceHostJsonFile = ObjectUtils.firstNonNull(project.getHostJsonFile(), new File(project.getBaseDirectory(), HOST_JSON));
        final File destHostJsonFile = Paths.get(project.getStagingFolder().getAbsolutePath(), HOST_JSON).toFile();
        copyFilesWithDefaultContent(sourceHostJsonFile, destHostJsonFile, DEFAULT_HOST_JSON);
        AzureMessager.getMessager().info(SAVE_SUCCESS + destHostJsonFile.getAbsolutePath());
    }

    private void copyLocalSettingsJson(FunctionProject project) throws IOException {
        AzureMessager.getMessager().info(LINE_FEED + SAVING_LOCAL_SETTINGS_JSON);
        final File sourceLocalSettingsJsonFile = ObjectUtils.firstNonNull(project.getLocalSettingsJsonFile(), new File(project.getBaseDirectory(), LOCAL_SETTINGS_JSON));
        final File destLocalSettingsJsonFile = Paths.get(project.getStagingFolder().getAbsolutePath(), LOCAL_SETTINGS_JSON).toFile();
        copyFilesWithDefaultContent(sourceLocalSettingsJsonFile, destLocalSettingsJsonFile, DEFAULT_LOCAL_SETTINGS_JSON);
        AzureMessager.getMessager().info(SAVE_SUCCESS + destLocalSettingsJsonFile.getAbsolutePath());
    }

    private static void copyFilesWithDefaultContent(File source, File dest, String defaultContent)
            throws IOException {
        if (source != null && source.exists()) {
            FileUtils.copyFile(source, dest);
        } else {
            FileUtils.write(dest, defaultContent, Charset.defaultCharset());
        }
    }

    private void writeObjectToFile(final ObjectWriter objectWriter, final Object object, final File targetFile)
            throws IOException {
        targetFile.getParentFile().mkdirs();
        targetFile.createNewFile();
        objectWriter.writeValue(targetFile, object);
    }

    private ObjectWriter getObjectWriter() {
        final DefaultPrettyPrinter.Indenter indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE.withLinefeed(StringUtils.LF);
        final PrettyPrinter prettyPrinter = new DefaultPrettyPrinter().withObjectIndenter(indenter);
        return new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writer(prettyPrinter);
    }

    private void copyJarsToStageDirectory(FunctionProject project) throws IOException {
        final String stagingDirectory = project.getStagingFolder().getAbsolutePath();
        AzureMessager.getMessager().info(LINE_FEED + COPY_JARS + stagingDirectory);
        final File libFolder = Paths.get(stagingDirectory, "lib").toFile();
        if (libFolder.exists()) {
            FileUtils.cleanDirectory(libFolder);
        }
        for (final File file : project.getDependencies()) {
            FileUtils.copyFileToDirectory(file, libFolder);
        }
        FileUtils.copyFileToDirectory(project.getArtifactFile(), new File(stagingDirectory));
        AzureMessager.getMessager().info(COPY_SUCCESS);
    }

    private void trackFunctionProperties(Map<String, FunctionConfiguration> configMap) {
        AzureTelemetry.getActionContext().setProperty(TRIGGER_TYPE, StringUtils.join(getFunctionBindingList(configMap), ","));
    }

    private List<String> getFunctionBindingList(Map<String, FunctionConfiguration> configMap) {
        return configMap.values().stream().flatMap(configuration -> configuration.getBindings().stream())
                .map(Binding::getType)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    protected Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
        final Set<BindingEnum> result = new HashSet<>();
        configMap.values().forEach(configuration -> configuration.getBindings().
                forEach(binding -> result.add(binding.getBindingEnum())));
        return result;
    }

    protected boolean isInstallingExtensionNeeded(boolean skipInstallExtensions, FunctionProject project, Set<BindingEnum> bindingTypes) {
        if (skipInstallExtensions) {
            AzureMessager.getMessager().info(SKIP_INSTALL_EXTENSIONS_FLAG);
            return false;
        }
        final JsonObject hostJson = readHostJson(project);
        final String extensionBundleId = Optional.ofNullable(hostJson)
                .map(host -> host.getAsJsonObject(EXTENSION_BUNDLE))
                .map(extensionBundle -> extensionBundle.get("id"))
                .map(JsonElement::getAsString).orElse(null);
        if (StringUtils.equalsAnyIgnoreCase(extensionBundleId, EXTENSION_BUNDLE_ID, EXTENSION_BUNDLE_PREVIEW_ID)) {
            AzureMessager.getMessager().info(SKIP_INSTALL_EXTENSIONS_BUNDLE);
            return false;
        }
        final boolean isNonHttpTriggersExist = bindingTypes.stream().anyMatch(binding ->
                !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
        if (!isNonHttpTriggersExist) {
            AzureMessager.getMessager().info(SKIP_INSTALL_EXTENSIONS_HTTP);
            return false;
        }
        return true;
    }

    protected JsonObject readHostJson(FunctionProject project) {
        final File hostJson = ObjectUtils.firstNonNull(project.getHostJsonFile(), new File(project.getHostJsonFile(), HOST_JSON));
        try (final FileInputStream fis = new FileInputStream(hostJson);
             final Scanner scanner = new Scanner(new BOMInputStream(fis))) {
            final String jsonRaw = scanner.useDelimiter("\\Z").next();
            return JsonParser.parseString(jsonRaw).getAsJsonObject();
        } catch (IOException e) {
            return null;
        }
    }
}
