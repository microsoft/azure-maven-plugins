/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.maven.function.template.FunctionTemplate;
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

/**
 * Add new Azure Function to existing project
 */
@Mojo(name = "add")
public class AddMojo extends AbstractFunctionMojo {
    @Parameter(defaultValue = "${project.baseDir}", readonly = true, required = true)
    protected String baseDir;

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

    public String getFunctionPackageName() {
        return functionPackageName;
    }

    public String getFunctionName() {
        return StringUtils.capitalise(functionName);
    }

    public String getFunctionTemplate() {
        return functionTemplate;
    }

    protected String getBaseDir() {
        return baseDir;
    }

    protected String getSourceRoot() {
        return compileSourceRoots == null || compileSourceRoots.isEmpty() ?
                Paths.get(getBaseDir(), "src", "main", "java").toString() :
                compileSourceRoots.get(0);
    }

    protected void setFunctionPackageName(String functionPackageName) {
        this.functionPackageName = functionPackageName;
    }

    protected void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    protected void setFunctionTemplate(String functionTemplate) {
        this.functionTemplate = functionTemplate;
    }

    @Override
    protected void doExecute() throws Exception {
        final FunctionTemplate[] templates = loadAllFunctionTemplates();

        final FunctionTemplate template = getFunctionTemplate(templates);

        final String newFunctionClass = generateNewFunctionClass(template);

        saveFunctionToFile(newFunctionClass);
    }

    protected FunctionTemplate[] loadAllFunctionTemplates() throws Exception {
        getLog().info("");
        getLog().info("Step 1 of 4: Load all function templates");
        try (final InputStream is = AddMojo.class.getResourceAsStream("/templates.json")) {
            final String templatesJsonStr = IOUtil.toString(is);
            final ObjectMapper objectMapper = new ObjectMapper();
            final FunctionTemplate[] templates = objectMapper.readValue(templatesJsonStr, FunctionTemplate[].class);
            getLog().info("Successfully loaded all function templates");
            return templates;
        } catch (Exception e) {
            getLog().error("Failed to load all function templates.");
            throw e;
        }
    }

    protected FunctionTemplate getFunctionTemplate(final FunctionTemplate[] templates) throws Exception {
        getLog().info("");
        getLog().info("Step 2 of 4: Find chosen function template");
        final List<String> availableTemplateNames = Arrays.stream(templates)
                .map(t -> t.getMetadata().getName())
                .collect(Collectors.toList());
        assureInputFromUser("Function Template", getFunctionTemplate(), availableTemplateNames,
                this::setFunctionTemplate);

        final String templateName = getFunctionTemplate();
        getLog().info("Chosen function template: " + templateName);
        final Optional<FunctionTemplate> template = Arrays.stream(templates)
                .filter(t -> t.getMetadata().getName().equalsIgnoreCase(templateName))
                .findFirst();
        if (!template.isPresent()) {
            throw new Exception("Function template not found: " + templateName);
        }
        getLog().info("Successfully found function template: " + templateName);
        return template.get();
    }

    protected String generateNewFunctionClass(final FunctionTemplate template) {
        getLog().info("");
        getLog().info("Step 3 of 4: Prepare required parameters");

        final Map<String, String> propMap = new HashMap<>();

        getLog().info(format("Common parameter [%s]", "Function Name"));
        assureInputFromUser("Function Name",
                getFunctionName(),
                str -> isNotEmpty(str) && isIdentifier(str) && !isKeyword(str),
                this::setFunctionName);

        getLog().info(format("Common parameter [%s]", "Package Name"));
        assureInputFromUser("Package Name",
                getFunctionPackageName(),
                str -> isNotEmpty(str) && isName(str),
                this::setFunctionPackageName);

        propMap.put("functionName", getFunctionName());
        propMap.put("packageName", getFunctionPackageName());

        for (final String property : template.getMetadata().getUserPrompt()) {
            getLog().info(format("Trigger specific parameter [%s]", property));
            assureInputFromUser(property, null, str -> isNotEmpty(str), str -> propMap.put(property, str));
        }

        String ret = template.getFiles().get("function.java");
        for (final Map.Entry<String, String> entry : propMap.entrySet()) {
            ret = ret.replace(String.format("$%s$", entry.getKey()), entry.getValue());
        }
        return ret;
    }

    protected void saveFunctionToFile(final String newFunctionClass) throws Exception {
        getLog().info("");
        getLog().info("Step 4 of 4: Saving function to file");

        final String sourceRoot = getSourceRoot();

        final String[] packageName = getFunctionPackageName().split("\\.");
        final File packageDir = Paths.get(sourceRoot, packageName).toFile();

        final String functionName = getFunctionName() + ".java";
        final File targetFile = new File(packageDir, functionName);

        if (targetFile.exists()) {
            final String message = format("Function already exists at %s. Please specify a different function name.",
                    targetFile.getAbsolutePath());
            throw new FileAlreadyExistsException(message);
        }

        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }

        try (final OutputStream os = new FileOutputStream(targetFile)) {
            copy(newFunctionClass, os);
        }

        getLog().info("Successfully saved new function at " + targetFile.getAbsolutePath());
    }

    protected void assureInputFromUser(final String propertyName, final String initValue, final List<String> options,
                                       final Consumer<String> setter) {
        if (options.stream().anyMatch(o -> o.equalsIgnoreCase(initValue))) {
            return;
        }

        out.printf("Choose from below options for %s.%n", propertyName);
        for (int i = 0; i < options.size(); i++) {
            out.printf("%d. %s%n", i, options.get(i));
        }

        assureInputFromUser(propertyName, "", str -> {
            try {
                final int index = Integer.parseInt(str);
                return 0 <= index && index < options.size();
            } catch (Exception e) {
                return false;
            }
        }, str -> {
            final int index = Integer.parseInt(str);
            setter.accept(options.get(index));
        });
    }

    protected void assureInputFromUser(final String propertyName, final String initValue,
                                       final Function<String, Boolean> validator, final Consumer<String> setter) {
        if (validator.apply(initValue)) {
            setter.accept(initValue);
            return;
        }

        final Scanner scanner = getScanner();

        while (true) {
            out.printf("Enter value for %s: ", propertyName);
            out.flush();
            try {
                final String input = scanner.next();
                if (validator.apply(input)) {
                    setter.accept(input);
                    break;
                } else {
                    getLog().warn("Invalid input: " + input);
                }
            } catch (Exception e) {
                getLog().warn("Invalid input.");
            }
        }
    }

    protected Scanner getScanner() {
        return new Scanner(System.in, "UTF-8");
    }
}
