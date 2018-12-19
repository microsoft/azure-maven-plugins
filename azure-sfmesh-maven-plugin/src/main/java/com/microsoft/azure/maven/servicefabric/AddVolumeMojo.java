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
 * Goal which adds a volume resource to a project.
 */
@Mojo(name = "addvolume", defaultPhase = LifecyclePhase.NONE)
public class AddVolumeMojo extends AbstractMojo{

    /**
     * schema version of the network yaml to be generated
    */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Name of the volume
    */
    @Parameter(property = "volumeName", required = true)
    String volumeName;

    /**
     * Name of the volume
    */
    @Parameter(property = "volumeDescription", defaultValue = Constants.DEFAULT_VOLUME_DESCRIPTION)
    String volumeDescription;

    /**
     * Name of the volme provider
    */
    @Parameter(property = "volumeProvider", defaultValue = Constants.DEFAULT_VOLUME_PROVIDER)
    String volumeProvider;

    /**
     * Name of the volume share
    */
    @Parameter(property = "volumeShareName", required = true)
    String volumeShareName;

    /**
     * Name of the volume account
    */
    @Parameter(property = "volumeAccountName", required = true)
    String volumeAccountName;

    /**
     * Key of the volume account 
    */
    @Parameter(property = "volumeAccountKey", required = true)
    String volumeAccountKey;

    public Log logger  = getLog();
    
    @Override
    public void execute() throws MojoFailureException {
        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder does not exist." +
                " Please run init goal before running this goal!");
        } else {
            if (Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "volume_" + volumeName + ".yaml"))){
                throw new MojoFailureException("Volume Resource with the specified name already exists");
            }
            final InputStream resource =
                this.getClass().getClassLoader().getResourceAsStream(Constants.VOLUME_RESOURCE_NAME);
            try {
                String volumeContent = IOUtil.toString(resource, "UTF-8");
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "SCHEMA_VERSION", schemaVersion, Constants.VOLUME_RESOURCE_NAME);
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "VOLUME_NAME", volumeName, Constants.VOLUME_RESOURCE_NAME);
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "VOLUME_DESCRIPTION", volumeDescription, Constants.VOLUME_RESOURCE_NAME);
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "VOLUME_PROVIDER", volumeProvider, Constants.VOLUME_RESOURCE_NAME);
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "VOLUME_ACCOUNT_NAME", volumeAccountName, Constants.VOLUME_RESOURCE_NAME);
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "VOLUME_ACCOUNT_KEY", volumeAccountKey, Constants.VOLUME_RESOURCE_NAME);
                volumeContent = Utils.replaceString(logger, volumeContent,
                    "VOLUME_SHARE_NAME", volumeShareName, Constants.VOLUME_RESOURCE_NAME);
                FileUtils.fileWrite(Utils.getPath(appResourcesDirectory,
                    "volume_" + volumeName + ".yaml"), volumeContent);
                logger.debug(String.format("Wrote %s volume content to output", volumeName));
                TelemetryHelper.sendEvent(TelemetryEventType.ADDVOLUME,
                    String.format("Added volume with name: %s", volumeName), logger);
            } catch (IOException e) {
                logger.error(e);
                throw new MojoFailureException("Error while writing output");
            } 
        }
    }
}
