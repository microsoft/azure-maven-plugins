/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


package com.microsoft.azure.maven.function;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.microsoft.azure.common.function.LocalRunHandler;

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
		final String enableDebug = System.getProperty("enableDebug");
		final boolean debugEnabled = StringUtils.isNotEmpty(localDebugConfig) && StringUtils.isNotEmpty(enableDebug) && enableDebug.equalsIgnoreCase("true");
		new LocalRunHandler(context.getDeploymentStagingDirectoryPath(), debugEnabled ? localDebugConfig : null).execute();
	}

}
