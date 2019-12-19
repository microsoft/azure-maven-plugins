/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.function.template.BindingTemplate;
import com.microsoft.azure.maven.function.template.BindingsTemplate;
import com.microsoft.azure.maven.function.template.FunctionSettingTemplate;
import com.microsoft.azure.maven.function.template.FunctionTemplate;
import com.microsoft.azure.maven.function.template.FunctionTemplates;
import com.microsoft.azure.maven.function.template.TemplateResources;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.System.out;
import static javax.lang.model.SourceVersion.isName;

/**
 * Create new Azure Functions (as Java class) and add to current project.
 */
public class AddFunctionHandler  {
    public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	public void setCompileSourceRoots(List<String> compileSourceRoots) {
		this.compileSourceRoots = compileSourceRoots;
	}

	public void setInteractiveMode(boolean isInteractiveMode) {
		this.isInteractiveMode = isInteractiveMode;
	}

	public static final String LOAD_TEMPLATES = "Step 1 of 4: Load all function templates";
    public static final String LOAD_TEMPLATES_DONE = "Successfully loaded all function templates";
    public static final String LOAD_TEMPLATES_FAIL = "Failed to load all function templates.";
    public static final String FIND_TEMPLATE = "Step 2 of 4: Select function template";
    public static final String FIND_TEMPLATE_DONE = "Successfully found function template: ";
    public static final String FIND_TEMPLATE_FAIL = "Function template not found: ";
    public static final String LOAD_BINDING_TEMPLATES_FAIL = "Failed to load function binding template.";
    public static final String PREPARE_PARAMS = "Step 3 of 4: Prepare required parameters";
    public static final String FOUND_VALID_VALUE = "Found valid value. Skip user input.";
    public static final String SAVE_FILE = "Step 4 of 4: Saving function to file";
    public static final String SAVE_FILE_DONE = "Successfully saved new function at ";
    public static final String FILE_EXIST = "Function already exists at %s. Please specify a different function name.";
    public static final String DEFAULT_INPUT_ERROR_MESSAGE = "Invalid input, please check and try again.";
    public static final String PROMPT_STRING_WITH_DEFAULTVALUE = "Enter value for %s(Default: %s): ";
    public static final String PROMPT_STRING_WITHOUT_DEFAULTVALUE = "Enter value for %s: ";
    private static final String FUNCTION_NAME_REGEXP = "^[a-zA-Z][a-zA-Z\\d_\\-]*$";

    //region Properties

    private File basedir;
    private List<String> compileSourceRoots;

    private String functionPackageName;

    private String functionName;

    private String functionTemplate;
	private boolean isInteractiveMode;

    //endregion

    //region Getter and Setter

    public String getFunctionPackageName() {
        return functionPackageName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getClassName() {
        return getFunctionName().replace('-', '_');
    }

    public String getFunctionTemplate() {
        return functionTemplate;
    }

    protected String getBasedir() {
        return basedir.getAbsolutePath();
    }

    protected String getSourceRoot() {
        return compileSourceRoots == null || compileSourceRoots.isEmpty() ?
                Paths.get(getBasedir(), "src", "main", "java").toString() :
                compileSourceRoots.get(0);
    }

    public void setFunctionPackageName(String functionPackageName) {
        this.functionPackageName = StringUtils.lowerCase(functionPackageName);
    }

    public void setFunctionName(String functionName) {
        this.functionName = StringUtils.capitalize(functionName);
    }

    public void setFunctionTemplate(String functionTemplate) {
        this.functionTemplate = functionTemplate;
    }

    //endregion

    //region Entry Point

    public void execute() throws Exception {
        final List<FunctionTemplate> templates = loadAllFunctionTemplates();

        final FunctionTemplate template = getFunctionTemplate(templates);
        final BindingTemplate bindingTemplate = loadBindingTemplate(template.getTriggerType());

        final Map params = prepareRequiredParameters(template, bindingTemplate);

        final String newFunctionClass = substituteParametersInTemplate(template, params);

        saveNewFunctionToFile(newFunctionClass);
    }

    //endregion

    //region Load templates
    protected BindingTemplate loadBindingTemplate(String type) {
        try (final InputStream is = AddFunctionHandler.class.getResourceAsStream("/bindings.json")) {
            final String bindingsJsonStr = IOUtils.toString(is, "utf8");
            final BindingsTemplate bindingsTemplate = new ObjectMapper()
                .readValue(bindingsJsonStr, BindingsTemplate.class);
            return bindingsTemplate.getBindingTemplateByName(type);
        } catch (IOException e) {
        	Log.warn(LOAD_BINDING_TEMPLATES_FAIL);
            // Add mojo could work without Binding Template, just return null if binding load fail
            return null;
        }
    }

    protected List<FunctionTemplate> loadAllFunctionTemplates() throws Exception {
        Log.info("");
        Log.info(LOAD_TEMPLATES);

        try (final InputStream is = AddFunctionHandler.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtils.toString(is, "utf8");
            final List<FunctionTemplate> templates = parseTemplateJson(templatesJsonStr);
            Log.info(LOAD_TEMPLATES_DONE);
            return templates;
        } catch (Exception e) {
        	Log.error(LOAD_TEMPLATES_FAIL);
            throw e;
        }
    }

    protected List<FunctionTemplate> parseTemplateJson(final String templateJson) throws Exception {
        final FunctionTemplates templates = new ObjectMapper().readValue(templateJson, FunctionTemplates.class);
        return templates.getTemplates();
    }

    //endregion

    //region Get function template
    protected FunctionTemplate getFunctionTemplate(final List<FunctionTemplate> templates) throws Exception {
    	Log.info("");
    	Log.info(FIND_TEMPLATE);

        if (!isInteractiveMode) {
            assureInputInBatchMode(getFunctionTemplate(),
                str -> getTemplateNames(templates)
                    .stream()
                    .filter(Objects::nonNull)
                    .anyMatch(o -> o.equalsIgnoreCase(str)),
                this::setFunctionTemplate,
                true);
        } else {
            assureInputFromUser("template for new function",
                getFunctionTemplate(),
                getTemplateNames(templates),
                this::setFunctionTemplate);
        }

        return findTemplateByName(templates, getFunctionTemplate());
    }

    protected List<String> getTemplateNames(final List<FunctionTemplate> templates) {
        return templates.stream().map(t -> t.getMetadata().getName()).collect(Collectors.toList());
    }

    protected FunctionTemplate findTemplateByName(final List<FunctionTemplate> templates, final String templateName)
            throws Exception {
    	Log.info("Selected function template: " + templateName);
        final Optional<FunctionTemplate> template = templates.stream()
            .filter(t -> t.getMetadata().getName().equalsIgnoreCase(templateName))
            .findFirst();

        if (template.isPresent()) {
        	Log.info(FIND_TEMPLATE_DONE + templateName);
            return template.get();
        }

        throw new Exception(FIND_TEMPLATE_FAIL + templateName);
    }

    //endregion

    //region Prepare parameters

    protected Map<String, String> prepareRequiredParameters(final FunctionTemplate template,
                                                            final BindingTemplate bindingTemplate)
        throws AzureExecutionException {
        Log.info("");
        Log.info(PREPARE_PARAMS);

        prepareFunctionName();

        preparePackageName();

        final Map<String, String> params = new HashMap<>();
        params.put("functionName", getFunctionName());
        params.put("className", getClassName());
        params.put("packageName", getFunctionPackageName());

        prepareTemplateParameters(template, bindingTemplate, params);

        displayParameters(params);

        return params;
    }

    protected void prepareFunctionName() throws AzureExecutionException {
    	Log.info("Common parameter [Function Name]: name for both the new function and Java class");

        if (!isInteractiveMode) {
            assureInputInBatchMode(getFunctionName(),
                str -> StringUtils.isNotEmpty(str) && str.matches(FUNCTION_NAME_REGEXP),
                this::setFunctionName,
                true);
        } else {
            assureInputFromUser("Enter value for Function Name: ",
                getFunctionName(),
                str -> StringUtils.isNotEmpty(str) && str.matches(FUNCTION_NAME_REGEXP),
                "Function name must start with a letter and can contain letters, digits, '_' and '-'",
                this::setFunctionName);
        }
    }

    protected void preparePackageName() throws AzureExecutionException {
        Log.info("Common parameter [Package Name]: package name of the new Java class");

        if (!isInteractiveMode) {
            assureInputInBatchMode(getFunctionPackageName(),
                str -> StringUtils.isNotEmpty(str) && isName(str),
                this::setFunctionPackageName,
                true);
        } else {
            assureInputFromUser("Enter value for Package Name: ",
                getFunctionPackageName(),
                str -> StringUtils.isNotEmpty(str) && isName(str),
                "Input should be a valid Java package name.",
                this::setFunctionPackageName);
        }
    }

    protected Map<String, String> prepareTemplateParameters(final FunctionTemplate template,
                                                            final BindingTemplate bindingTemplate,
                                                            final Map<String, String> params)
        throws AzureExecutionException {
        for (final String property : template.getMetadata().getUserPrompt()) {
            String initValue = System.getProperty(property);
            final List<String> options = getOptionsForUserPrompt(property);
            final FunctionSettingTemplate settingTemplate = bindingTemplate == null ?
                null : bindingTemplate.getSettingTemplateByName(property);
            final String helpMessage = (settingTemplate != null && settingTemplate.getHelp() != null) ?
                settingTemplate.getHelp() : "";

                Log.info(format("Trigger specific parameter [%s]:%s", property,
                TemplateResources.getResource(helpMessage)));
            if (!isInteractiveMode) {
                if (options != null && options.size() > 0) {
                    final String foundElement = findElementInOptions(options, initValue);
                    initValue = foundElement == null ? options.get(0) : foundElement;
                }

                assureInputInBatchMode(
                    initValue,
                    StringUtils::isNotEmpty,
                    str -> params.put(property, str),
                    false
                );
            } else {
                if (options == null) {
                    params.put(property, getStringInputFromUser(property, initValue, settingTemplate));
                } else {
                    assureInputFromUser(
                        format("the value for %s: ", property),
                        System.getProperty(property),
                        options,
                        str -> params.put(property, str)
                    );
                }
            }
        }

        return params;
    }

    protected String getStringInputFromUser(String attributeName, String initValue, FunctionSettingTemplate template) {
        final String defaultValue = template == null ? null : template.getDefaultValue();
        final Function<String, Boolean> validator = getStringInputValidator(template);

        if (validator.apply(initValue)) {
        	Log.info(FOUND_VALID_VALUE);
            return initValue;
        }

        final Scanner scanner = getScanner();
        while (true) {
            out.printf(getStringInputPromptString(attributeName, defaultValue));
            out.flush();
            final String input = scanner.nextLine();
            if (validator.apply(input)) {
                return input;
            } else if (StringUtils.isNotEmpty(defaultValue) && StringUtils.isEmpty(input)) {
                return defaultValue;
            }
            Log.warn(getStringInputErrorMessage(template));
        }
    }

    protected String getStringInputErrorMessage(FunctionSettingTemplate template) {
        return (template != null && template.getErrorText() != null) ?
            TemplateResources.getResource(template.getErrorText()) : DEFAULT_INPUT_ERROR_MESSAGE;
    }

    protected String getStringInputPromptString(String attributeName, String defaultValue) {
        return StringUtils.isBlank(defaultValue) ?
            String.format(PROMPT_STRING_WITHOUT_DEFAULTVALUE, attributeName) :
            String.format(PROMPT_STRING_WITH_DEFAULTVALUE, attributeName, defaultValue);
    }

    protected Function<String, Boolean> getStringInputValidator(FunctionSettingTemplate template) {
        final String regex = template == null ? null : template.getSettingRegex();
        if (regex == null) {
            return StringUtils::isNotEmpty;
        } else {
            return (attribute) -> StringUtils.isNotEmpty(attribute) && attribute.matches(regex);
        }
    }

    protected void displayParameters(final Map<String, String> params) {
    	Log.info("");
        Log.info("Summary of parameters for function template:");

        params.entrySet().stream().forEach(e -> Log.info(format("%s: %s", e.getKey(), e.getValue())));
    }

    //endregion

    //region Substitute parameters

    protected String substituteParametersInTemplate(final FunctionTemplate template, final Map<String, String> params) {
        String ret = template.getFiles().get("function.java");
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            ret = ret.replace(String.format("$%s$", entry.getKey()), entry.getValue());
        }
        return ret;
    }

    //endregion

    //region Save function to file

    protected void saveNewFunctionToFile(final String newFunctionClass) throws Exception {
    	Log.info("");
    	Log.info(SAVE_FILE);

        final File packageDir = getPackageDir();

        final File targetFile = getTargetFile(packageDir);

        createPackageDirIfNotExist(packageDir);

        saveToTargetFile(targetFile, newFunctionClass);

        Log.info(SAVE_FILE_DONE + targetFile.getAbsolutePath());
    }

    protected File getPackageDir() {
        final String sourceRoot = getSourceRoot();
        final String[] packageName = getFunctionPackageName().split("\\.");
        return Paths.get(sourceRoot, packageName).toFile();
    }

    protected File getTargetFile(final File packageDir) throws Exception {
        final String javaFileName = getClassName() + ".java";
        final File targetFile = new File(packageDir, javaFileName);
        if (targetFile.exists()) {
            throw new FileAlreadyExistsException(format(FILE_EXIST, targetFile.getAbsolutePath()));
        }
        return targetFile;
    }

    protected void createPackageDirIfNotExist(final File packageDir) {
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
    }

    protected void saveToTargetFile(final File targetFile, final String newFunctionClass) throws Exception {
        try (final OutputStream os = new FileOutputStream(targetFile)) {
            IOUtils.write(newFunctionClass, os, "utf8");
        }
    }

    //endregion

    //region Helper methods

    protected void assureInputFromUser(final String prompt, final String initValue, final List<String> options,
                                       final Consumer<String> setter) {
        final String option = findElementInOptions(options, initValue);
        if (option != null) {
            Log.info(FOUND_VALID_VALUE);
            setter.accept(option);
            return;
        }

        out.printf("Choose from below options as %s %n", prompt);
        for (int i = 0; i < options.size(); i++) {
            out.printf("%d. %s%n", i, options.get(i));
        }

        assureInputFromUser("Enter index to use: ", null,
            str -> {
                try {
                    final int index = Integer.parseInt(str);
                    return 0 <= index && index < options.size();
                } catch (Exception e) {
                    return false;
                }
            },
            "Invalid index.", str -> {
                final int index = Integer.parseInt(str);
                setter.accept(options.get(index));
            }
        );
    }

    protected void assureInputFromUser(final String prompt, final String initValue,
                                       final Function<String, Boolean> validator, final String errorMessage,
                                       final Consumer<String> setter) {
        if (validator.apply(initValue)) {
            Log.info(FOUND_VALID_VALUE);
            setter.accept(initValue);
            return;
        }

        final Scanner scanner = getScanner();

        while (true) {
            out.printf(prompt);
            out.flush();
            try {
                final String input = scanner.nextLine();
                if (validator.apply(input)) {
                    setter.accept(input);
                    break;
                }
            } catch (Exception ignored) {
            }
            // Reaching here means invalid input
            Log.warn(errorMessage);
        }
    }

    protected void assureInputInBatchMode(final String input, final Function<String, Boolean> validator,
                                          final Consumer<String> setter, final boolean required)
            throws AzureExecutionException {
        if (validator.apply(input)) {
            Log.info(FOUND_VALID_VALUE);
            setter.accept(input);
            return;
        }

        if (required) {
            throw new AzureExecutionException(String.format("invalid input: %s", input));
        } else {
            out.printf("The input is invalid. Use empty string.%n");
            setter.accept("");
        }
    }

    protected Scanner getScanner() {
        return new Scanner(System.in, "UTF-8");
    }

    @Nullable
    private String findElementInOptions(List<String> options, String item) {
        return options.stream()
            .filter(o -> o != null && o.equalsIgnoreCase(item))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    private List<String> getOptionsForUserPrompt(final String promptName) {
        // HTTP Trigger
        if ("authlevel".equalsIgnoreCase(promptName.trim())) {
            return Arrays.asList("ANONYMOUS", "FUNCTION", "ADMIN");
        }
        // Cosmos DB Trigger
        if ("createLeaseCollectionIfNotExists".equalsIgnoreCase(promptName.trim())) {
            return Arrays.asList("true", "false");
        }
        return null;
    }

    //endregion
}
