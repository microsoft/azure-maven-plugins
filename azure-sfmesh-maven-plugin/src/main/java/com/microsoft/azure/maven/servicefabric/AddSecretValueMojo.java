/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.servicefabric;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Goal which adds a secret value resource to a project.
 */
@Mojo(name = "addsecretvalue", defaultPhase = LifecyclePhase.NONE)
public class AddSecretValueMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;
    
    /**
     * schema version of the secret value yaml to be generated
    */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;

    /**
     * Name of the secretvalue
    */
    @Parameter(property = "secretValueName", required = true)
    String secretValueName;

    /**
     * Value of the secret
    */
    @Parameter(property = "secretValue", required = true)
    String secretValue;

    public Log logger = getLog();

    @Override
    public void execute() throws MojoFailureException {
        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder " +
                "does not exist. Please run init goal before running this goal!");
        }
        final String[] secretValueSplit = secretValueName.split("/", 2);
        if (Utils.checkIfExists(Utils.getPath(appResourcesDirectory,
            "secretvalue_" + secretValueSplit[0] + "_" + secretValueSplit[1] + ".yaml"))){
            throw new MojoFailureException("Secret Value Resource" +
                " with the specified name already exists");
        }
        String secretValueContent = new YamlContent.Builder()
                .addElement("SCHEMA_VERSION", schemaVersion)
                .addElement("SECRET_VALUE_NAME", secretValueName)
                .addElement("SECRET_VALUE", secretValue)
                .build(logger, Constants.SECRET_VALUE_RESOURCE_NAME);
        try {
            FileUtils.fileWrite(Utils.getPath(appResourcesDirectory,
                "secretvalue_" + secretValueSplit[0] + "_" + secretValueSplit[1] + ".yaml"), secretValueContent);
            logger.debug(String.format("Wrote %s secret value content to output", secretValueName));
            TelemetryHelper.sendEvent(TelemetryEventType.ADDSECRETVALUE,
                String.format("Added secret value with name: %s", secretValueName), logger);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException("Error while writing output");
        }
    }
}
