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

import java.io.IOException;

/**
 * Goal which creates initial application resource of a project.
 */
@Mojo(name = "init", defaultPhase = LifecyclePhase.NONE)
public class InitMojo extends AbstractMojo {

    /**
     * schema version of the network yaml to be generated
    */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Name of the application
    */
    @Parameter(property = "applicationName", required = true)
    String applicationName;

    /**
     * Description of the application
    */
    @Parameter(property = "applicationDescription", defaultValue = Constants.DEFAULT_APPLICATION_DESCRIPTION)
    String applicationDescription;

    private Log logger  = getLog();

    @Override
    public void execute() throws MojoFailureException{

        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            logger.debug(String.format("Creating service fabric resources " +
                "directory %s", serviceFabricResourcesDirectory));
            Utils.createDirectory(logger, serviceFabricResourcesDirectory);
        }
        if (!Utils.checkIfExists(appResourcesDirectory)){
            Utils.createDirectory(logger, appResourcesDirectory);
        }

        final String appContent = new YamlContent.Builder()
                .addElement("SCHEMA_VERSION", schemaVersion)
                .addElement("APP_NAME", applicationName)
                .addElement("APP_DESCRIPTION", applicationDescription)
                .build(logger, Constants.APPLICATION_RESOURCE_NAME);
        try {
            final String appYamlPath = Utils.getPath(appResourcesDirectory, "app_" + applicationName + ".yaml");
            if (Utils.checkIfExists(appYamlPath)){
                throw new MojoFailureException(String.format("App resource with the " +
                "name %s already exists", applicationName));
            } else {
                FileUtils.fileWrite(appYamlPath, appContent);
            }
            logger.debug(String.format("Wrote %s application content to output", applicationName));
            TelemetryHelper.sendEvent(TelemetryEventType.INIT, String.format("Added application " +
                "with name: %s", applicationName), logger);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException("Error while writing output");
        }
    }
}

