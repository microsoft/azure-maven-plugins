/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.microsoft.azure.common.function.AddFunctionHandler;

/**
 * Create new Azure Functions (as Java class) and add to current project.
 */
@Mojo(name = "add")
public class AddMojo extends AbstractFunctionMojo {

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
		AddFunctionHandler handler = new AddFunctionHandler();
		handler.setBasedir(this.basedir);
		handler.setCompileSourceRoots(compileSourceRoots);
		handler.setFunctionName(functionName);
		handler.setFunctionPackageName(functionPackageName);
		handler.setFunctionTemplate(functionTemplate);
		handler.setInteractiveMode(this.getSettings().isInteractiveMode());
		handler.execute();
	}

}
