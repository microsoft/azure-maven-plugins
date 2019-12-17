/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.maven.function.template.BindingTemplate;
import com.microsoft.azure.maven.function.template.BindingsTemplate;
import com.microsoft.azure.maven.function.template.FunctionSettingTemplate;
import com.microsoft.azure.maven.function.template.FunctionTemplate;
import com.microsoft.azure.maven.function.template.FunctionTemplates;
import com.microsoft.azure.maven.function.template.TemplateResources;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;

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
@Mojo(name = "add")
public class AddMojo extends AbstractFunctionMojo {

	private static final String FUNCTION_NAME_REGEXP = "^[a-zA-Z][a-zA-Z\\d_\\-]*$";

	// region Properties

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

	// endregion

	// region Getter and Setter

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
		return compileSourceRoots == null || compileSourceRoots.isEmpty()
				? Paths.get(getBasedir(), "src", "main", "java").toString()
				: compileSourceRoots.get(0);
	}

	protected void setFunctionPackageName(String functionPackageName) {
		this.functionPackageName = StringUtils.lowerCase(functionPackageName);
	}

	protected void setFunctionName(String functionName) {
		this.functionName = StringUtils.capitalize(functionName);
	}

	protected void setFunctionTemplate(String functionTemplate) {
		this.functionTemplate = functionTemplate;
	}

	@Override
	protected void doExecute() throws Exception {
		// TODO
	}

}
