/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.maven.function.template.FunctionTemplate;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.System.out;
import static javax.lang.model.SourceVersion.*;
import static org.codehaus.plexus.util.IOUtil.copy;
import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

import javax.annotation.Nullable;

/**
 * Create new Azure Functions (as Java class) and add to current project.
 */
@Mojo(name = "add")
public class AddMojo extends AbstractFunctionMojo {
    public static final String LOAD_TEMPLATES = "Step 1 of 4: Load all function templates";
    public static final String LOAD_TEMPLATES_DONE = "Successfully loaded all function templates";
    public static final String LOAD_TEMPLATES_FAIL = "Failed to load all function templates.";
    public static final String FIND_TEMPLATE = "Step 2 of 4: Select function template";
    public static final String FIND_TEMPLATE_DONE = "Successfully found function template: ";
    public static final String FIND_TEMPLATE_FAIL = "Function template not found: ";
    public static final String PREPARE_PARAMS = "Step 3 of 4: Prepare required parameters";
    public static final String FOUND_VALID_VALUE = "Found valid value. Skip user input.";
    public static final String SAVE_FILE = "Step 4 of 4: Saving function to file";
    public static final String SAVE_FILE_DONE = "Successfully saved new function at ";
    public static final String FILE_EXIST = "Function already exists at %s. Please specify a different function name.";
    private static final String FUNCTION_NAME_REGEXP = "^[a-zA-Z][a-zA-Z\\d_\\-]*$";

    //region Properties

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected File basedir;

    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
    protected List<String> compileSourceRoots;

    /**
     * Package name of the new function.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.package")
    protected String functionPackageName;

    /**
     * Name of the new function.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.name")
    protected String functionName;

    /**
     * Template for the new function
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.template")
    protected String functionTemplate;

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

    protected void setFunctionPackageName(String functionPackageName) {
        this.functionPackageName = StringUtils.lowerCase(functionPackageName);
    }

    protected void setFunctionName(String functionName) {
        this.functionName = StringUtils.capitalise(functionName);
    }

    protected void setFunctionTemplate(String functionTemplate) {
        this.functionTemplate = functionTemplate;
    }

    //endregion

    //region Entry Point

    @Override
    protected void doExecute() throws Exception {
        final List<FunctionTemplate> templates = loadAllFunctionTemplates();

        final FunctionTemplate template = getFunctionTemplate(templates);

        final Map params = prepareRequiredParameters(template);

        final String newFunctionClass = substituteParametersInTemplate(template, params);

        saveNewFunctionToFile(newFunctionClass);
    }

    //endregion

    //region Load all templates

    protected List<FunctionTemplate> loadAllFunctionTemplates() throws Exception {
        info("");
        info(LOAD_TEMPLATES);

        try (final InputStream is = AddMojo.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtil.toString(is);
            final List<FunctionTemplate> templates = parseTemplateJson(templatesJsonStr);
            info(LOAD_TEMPLATES_DONE);
            return templates;
        } catch (Exception e) {
            error(LOAD_TEMPLATES_FAIL);
            throw e;
        }
    }

    protected List<FunctionTemplate> parseTemplateJson(final String templateJson) throws Exception {
        final FunctionTemplate[] templates = new ObjectMapper().readValue(templateJson, FunctionTemplate[].class);
        return Arrays.asList(templates);
    }

    //endregion

    //region Get function template

    protected FunctionTemplate getFunctionTemplate(final List<FunctionTemplate> templates) throws Exception {
        info("");
        info(FIND_TEMPLATE);

        if (settings != null && !settings.isInteractiveMode()) {
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
        info("Selected function template: " + templateName);
        final Optional<FunctionTemplate> template = templates.stream()
                .filter(t -> t.getMetadata().getName().equalsIgnoreCase(templateName))
                .findFirst();

        if (template.isPresent()) {
            info(FIND_TEMPLATE_DONE + templateName);
            return template.get();
        }

        throw new Exception(FIND_TEMPLATE_FAIL + templateName);
    }

    //endregion

    //region Prepare parameters

    protected Map<String, String> prepareRequiredParameters(final FunctionTemplate template)
            throws MojoFailureException {
        info("");
        info(PREPARE_PARAMS);

        prepareFunctionName();

        preparePackageName();

        final Map<String, String> params = new HashMap<>();
        params.put("functionName", getFunctionName());
        params.put("className", getClassName());
        params.put("packageName", getFunctionPackageName());

        prepareTemplateParameters(template, params);

        displayParameters(params);

        return params;
    }

    protected void prepareFunctionName() throws MojoFailureException {
        info("Common parameter [Function Name]: name for both the new function and Java class");

        if (settings != null && !settings.isInteractiveMode()) {
            assureInputInBatchMode(getFunctionName(),
                    str -> isNotEmpty(str) && str.matches(FUNCTION_NAME_REGEXP),
                    this::setFunctionName,
                    true);
        } else {
            assureInputFromUser("Enter value for Function Name: ",
                    getFunctionName(),
                    str -> isNotEmpty(str) && str.matches(FUNCTION_NAME_REGEXP),
                    "Function name must start with a letter and can contain letters, digits, '_' and '-'",
                    this::setFunctionName);
        }
    }

    protected void preparePackageName() throws MojoFailureException {
        info("Common parameter [Package Name]: package name of the new Java class");

        if (settings != null && !settings.isInteractiveMode()) {
            assureInputInBatchMode(getFunctionPackageName(),
                    str -> isNotEmpty(str) && isName(str),
                    this::setFunctionPackageName,
                    true);
        } else {
            assureInputFromUser("Enter value for Package Name: ",
                    getFunctionPackageName(),
                    str -> isNotEmpty(str) && isName(str),
                    "Input should be a valid Java package name.",
                    this::setFunctionPackageName);
        }
    }

    protected Map<String, String> prepareTemplateParameters(final FunctionTemplate template,
                                                            final Map<String, String> params)
            throws MojoFailureException {
        for (final String property : template.getMetadata().getUserPrompt()) {
            info(format("Trigger specific parameter [%s]", property));

            final List<String> options = getOptionsForUserPrompt(property);
            if (settings != null && !settings.isInteractiveMode()) {
                String initValue = System.getProperty(property);
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
                    assureInputFromUser(
                            format("Enter value for %s: ", property),
                            System.getProperty(property),
                            StringUtils::isNotEmpty,
                            "Input should be a non-empty string.",
                            str -> params.put(property, str)
                    );
                } else {
                    assureInputFromUser(
                            format("Enter value for %s: ", property),
                            System.getProperty(property),
                            options,
                            str -> params.put(property, str)
                    );
                }
            }
        }

        return params;
    }

    protected void displayParameters(final Map<String, String> params) {
        info("");
        info("Summary of parameters for function template:");

        params.entrySet()
                .stream()
                .forEach(e -> info(format("%s: %s", e.getKey(), e.getValue())));
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
        info("");
        info(SAVE_FILE);

        final File packageDir = getPackageDir();

        final File targetFile = getTargetFile(packageDir);

        createPackageDirIfNotExist(packageDir);

        saveToTargetFile(targetFile, newFunctionClass);

        info(SAVE_FILE_DONE + targetFile.getAbsolutePath());
    }

    protected File getPackageDir() {
        final String sourceRoot = getSourceRoot();
        final String[] packageName = getFunctionPackageName().split("\\.");
        return Paths.get(sourceRoot, packageName).toFile();
    }

    protected File getTargetFile(final File packageDir) throws Exception {
        final String functionName = getClassName() + ".java";
        final File targetFile = new File(packageDir, functionName);
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
            copy(newFunctionClass, os);
        }
    }

    //endregion

    //region Helper methods

    protected void assureInputFromUser(final String prompt, final String initValue, final List<String> options,
                                       final Consumer<String> setter) {
        final String option = findElementInOptions(options, initValue);
        if (option != null) {
            info(FOUND_VALID_VALUE);
            setter.accept(option);
            return;
        }

        out.printf("Choose from below options as %s.%n", prompt);
        for (int i = 0; i < options.size(); i++) {
            out.printf("%d. %s%n", i, options.get(i));
        }

        assureInputFromUser("Enter index to use: ",
                null,
                str -> {
                    try {
                        final int index = Integer.parseInt(str);
                        return 0 <= index && index < options.size();
                    } catch (Exception e) {
                        return false;
                    }
                },
                "Invalid index.",
                str -> {
                    final int index = Integer.parseInt(str);
                    setter.accept(options.get(index));
                });
    }

    protected void assureInputFromUser(final String prompt, final String initValue,
                                       final Function<String, Boolean> validator, final String errorMessage,
                                       final Consumer<String> setter) {
        if (validator.apply(initValue)) {
            info(FOUND_VALID_VALUE);
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
            warning(errorMessage);
        }
    }

    protected void assureInputInBatchMode(final String input, final Function<String, Boolean> validator,
                                          final Consumer<String> setter, final boolean required)
            throws MojoFailureException {
        if (validator.apply(input)) {
            info(FOUND_VALID_VALUE);
            setter.accept(input);
            return;
        }

        if (required) {
            throw new MojoFailureException(String.format("invalid input: %s", input));
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
        if ("authlevel".equalsIgnoreCase(promptName.trim())) {
            return Arrays.asList("ANONYMOUS", "FUNCTION", "ADMIN");
        }
        return null;
    }

    //endregion
}
