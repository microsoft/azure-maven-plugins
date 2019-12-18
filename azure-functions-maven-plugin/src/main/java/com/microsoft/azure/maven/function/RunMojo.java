/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.maven.function;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.microsoft.azure.common.function.RunHandler;

@Mojo(name = "run")
public class RunMojo extends AbstractFunctionMojo {

	  /**
     * Config String for local debug
     *
     * @since 1.0.0-beta-7
     */
    @Parameter(property = "localDebugConfig", defaultValue = "transport=dt_socket,server=y,suspend=n,address=5005")
    protected String localDebugConfig;

	@Override
	protected void doExecute() throws Exception {
		new RunHandler(context, localDebugConfig).execute();
	}

}
