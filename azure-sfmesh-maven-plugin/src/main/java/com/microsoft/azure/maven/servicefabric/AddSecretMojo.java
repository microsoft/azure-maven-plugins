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
 * Goal which adds a secret resource to a project.
 */
@Mojo(name = "addsecret", defaultPhase = LifecyclePhase.NONE)
public class AddSecretMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * schema version of the secret yaml to be generated
     */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;

    /**
     * Name of the secret
     */
    @Parameter(property = "secretName", required = true)
    String secretName;

    /**
     * Description of the secret
     */
    @Parameter(property = "secretDescription", defaultValue = Constants.DEFAULT_SECRET_DESCRIPTION)
    String secretDescription;

    /**
     * Kind of the secret
     */
    @Parameter(property = "secretKind", defaultValue = Constants.DEFAULT_SECRET_KIND)
    String secretKind;

    /**
     * Content type of the secret
     */
    @Parameter(property = "secretContentType", defaultValue = Constants.DEFAULT_SECRET_CONTENT_TYPE)
    String secretContentType;

    public Log logger  = getLog();

    @Override
    public void execute() throws MojoFailureException {
        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder " +
                    "does not exist. Please run init goal before running this goal!");
        }
        if (Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "secret_" + secretName + ".yaml"))){
            throw new MojoFailureException("Secret Resource with the specified name already exists");
        }

        final String secretContent = new YamlContent.Builder()
                .addElement("SCHEMA_VERSION", schemaVersion)
                .addElement("SECRET_NAME", secretName)
                .addElement("SECRET_DESCRIPTION", secretDescription)
                .addElement("SECRET_CONTENT_TYPE", secretContentType)
                .addElement("SECRET_KIND", secretKind)
                .build(logger, Constants.SECRET_RESOURCE_NAME);
        try {
            FileUtils.fileWrite(Utils.getPath(appResourcesDirectory,
                    "secret_" + secretName + ".yaml"), secretContent);
            logger.debug(String.format("Wrote %s secret content to output", secretName));
            TelemetryHelper.sendEvent(TelemetryEventType.ADDSECRET,
                    String.format("Added secret with name: %s", secretName), logger);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException("Error while writing output");
        }
    }
}
