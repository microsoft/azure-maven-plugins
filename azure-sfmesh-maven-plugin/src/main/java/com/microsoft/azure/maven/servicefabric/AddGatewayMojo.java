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
        } else {
            if (Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "gateway_" + gatewayName + ".yaml"))){
                throw new MojoFailureException("Gateway Resource with the specified name already exists");
            }
            final InputStream resource =
                this.getClass().getClassLoader().getResourceAsStream(Constants.GATEWAY_RESOURCE_NAME);

            try {
                String gatewayContent = IOUtil.toString(resource, "UTF-8");
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "SCHEMA_VERSION", schemaVersion, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "GATEWAY_NAME", gatewayName, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "GATEWAY_DESCRIPTION", gatewayDescription, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "SOURCE_NETWORK", sourceNetwork, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "DESTINATION_NETWORK", destinationNetwork, Constants.GATEWAY_RESOURCE_NAME);
                if (tcpName.equals(Constants.DEFAULT_TCP_NAME)){
                    tcpName = listenerName + "Config";
                }
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "TCP_NAME", tcpName, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "PORT", tcpPort, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "APPLICATION_NAME", applicationName, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "SERVICE_NAME", serviceName, Constants.GATEWAY_RESOURCE_NAME);
                gatewayContent = Utils.replaceString(logger, gatewayContent,
                    "LISTENER_NAME", listenerName, Constants.GATEWAY_RESOURCE_NAME);
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
    }
}
