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
 * Goal which adds a gateway resource to a project.
 */
@Mojo(name = "addgateway", defaultPhase = LifecyclePhase.NONE)
public class AddGatewayMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * schema version of the gateway yaml to be generated
    */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;
    

    /**
     * Name of the gateway
    */
    @Parameter(property = "gatewayName", required = true)
    String gatewayName;

    /**
     * Description of the gateway
    */
    @Parameter(property = "gatewayDescription", defaultValue = Constants.DEFAULT_GATEWAY_DESCRIPTION)
    String gatewayDescription;

    /**
     * Source network of gateway
    */
    @Parameter(property = "sourceNetwork", required = true)
    String sourceNetwork;

    /**
     * Destination network of gateway
    */
    @Parameter(property = "destinationNetwork", required = true)
    String destinationNetwork;

    /**
     * Name of the exposed endpoint
    */
    @Parameter(property = "tcpName", defaultValue = Constants.DEFAULT_TCP_NAME)
    String tcpName;

    /**
     * Name of the application
    */
    @Parameter(property = "applicationName", required = true)
    String applicationName;

    /**
     * Name of the service
    */
    @Parameter(property = "serviceName", required = true)
    String serviceName;

    /**
     * Name of the listener
    */
    @Parameter(property = "listenerName", required = true)
    String listenerName;
    
    /**
     * Port to be on the listener of the service
     */
    @Parameter(property = "tcpPort", required = true)
    String tcpPort;

    public Log logger  = getLog();

    @Override
    public void execute() throws MojoFailureException {

        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);

        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder does not exist" +
                " Please run init goal before running this goal!");
        }
        if (Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "gateway_" + gatewayName + ".yaml"))){
            throw new MojoFailureException("Gateway Resource with the specified name already exists");
        }

        final String gatewayContent = new YamlContent.Builder()
                .addElement("SCHEMA_VERSION", schemaVersion)
                .addElement("GATEWAY_NAME", gatewayName)
                .addElement("GATEWAY_DESCRIPTION", gatewayDescription)
                .addElement("SOURCE_NETWORK", sourceNetwork)
                .addElement("DESTINATION_NETWORK", destinationNetwork)
                .addElement("TCP_NAME", getTcpName())
                .addElement("PORT", tcpPort)
                .addElement("APPLICATION_NAME", applicationName)
                .addElement("SERVICE_NAME", serviceName)
                .addElement("LISTENER_NAME", listenerName)
                .build(logger, Constants.GATEWAY_RESOURCE_NAME);
        try {
            FileUtils.fileWrite(Utils.getPath(appResourcesDirectory,
                "gateway_" + gatewayName + ".yaml"), gatewayContent);
            logger.debug(String.format("Wrote %s gateway content to output", gatewayName));
            TelemetryHelper.sendEvent(TelemetryEventType.ADDGATEWAY,
                String.format("Added gateway with name: %s", gatewayName), logger);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException("Error while writing output");
        }
    }

    String getTcpName(){
        if (tcpName.equals(Constants.DEFAULT_TCP_NAME)){
            tcpName = listenerName + "Config";
        }
        return tcpName;
    }
}
