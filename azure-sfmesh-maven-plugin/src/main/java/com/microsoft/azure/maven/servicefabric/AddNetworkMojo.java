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
 * Goal which adds a network resource to a project.
 */
@Mojo(name = "addnetwork", defaultPhase = LifecyclePhase.NONE)
public class AddNetworkMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;
    
    /**
     * schema version of the network yaml to be generated
    */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;

    /**
     * Name of the network
    */
    @Parameter(property = "networkName", required = true)
    String networkName;
    
    /**
     * Description of the network
    */
    @Parameter(property = "networkDescription", defaultValue = Constants.DEFAULT_NETWORK_DESCRIPTION)
    String networkDescription;

    /**
     * Kind of the network
     */
    @Parameter(property = "networkKind", defaultValue = Constants.DEFAULT_NETWORK_KIND)
    String networkKind;

    /**
     * Address prefix of the subnet
     */
    @Parameter(property = "networkAddressPrefix", required = true)
    String networkAddressPrefix;

    public Log logger  = getLog();

    @Override
    public void execute() throws MojoFailureException {
        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder does" +
                "not exist. Please run init goal before running this goal!");
        }
        if (Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "network_" + networkName + ".yaml"))){
            throw new MojoFailureException("Network Resource with the specified name already exists");
        }

        final String networkContent = new YamlContent.Builder()
                .addElement("SCHEMA_VERSION", schemaVersion)
                .addElement("NETWORK_NAME", networkName)
                .addElement("NETWORK_DESCRIPTION", networkDescription)
                .addElement("ADDRESS_PREFIX", networkAddressPrefix)
                .addElement("NETWORK_KIND", networkKind)
                .build(logger, Constants.NETWORK_RESOURCE_NAME);
        try {
            FileUtils.fileWrite(Utils.getPath(appResourcesDirectory,
                "network_" + networkName + ".yaml"), networkContent);
            logger.debug(String.format("Wrote %s network content to output", networkName));
            TelemetryHelper.sendEvent(TelemetryEventType.ADDNETWORK,
                String.format("Added network with name: %s", networkName), logger);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException("Error while writing output");
        }
    }
}
