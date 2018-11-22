package com.microsoft.azure.maven.servicefabric;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugin.logging.Log;
/**
 * Goal which deploys application to mesh
 */
@Mojo( name = "deploy", defaultPhase = LifecyclePhase.NONE )
public class DeployMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Comma seperated resource files or the directory in which the resource files are present
    */
    @Parameter(property = "inputYamlFilePaths", defaultValue = Constants.SERVICE_FABRIC_RESOURCES_PATH)
    String inputYamlFilePaths;

    /**
     * Name of the resource group
    */
    @Parameter(property = "resourceGroup", defaultValue = Constants.DEFAULT_RESOURCE_GROUP)
    String resourceGroup;

    /**
     * Location of the resource group
    */
    @Parameter(property = "location", defaultValue = Constants.DEFAULT_LOCATION)
    String location;

    public Log logger  = getLog();

	@Override
	public void execute() throws MojoFailureException {
        String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        if(!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder does not exist. Please run init goal before running this goal!");
        }
        if(inputYamlFilePaths.equals(Constants.SERVICE_FABRIC_RESOURCES_PATH)){
            inputYamlFilePaths = Utils.getServicefabricResourceDirectory(logger, project);
        }

        Utils.checkazinstallation(logger);

        if(resourceGroup.equals(Constants.DEFAULT_RESOURCE_GROUP)){
            throw new MojoFailureException("Resource group is not provided. Please provide a resource group name");
        }

        // Create resource group
        logger.info("Creating Resource Group");
        Utils.executeCommand(logger, String.format("az group create --name %s --location %s", resourceGroup, location));
        // Perform deployment
        logger.info("Performing deployment");
        Utils.executeCommand(logger, String.format("az mesh deployment create --resource-group %s --input-yaml-file-paths %s  --parameters \"{'location': {'value': '%s'}}\"", resourceGroup, inputYamlFilePaths, location));
        TelemetryHelper.sendEvent(TelemetryEventType.DEPLOYMESH, String.format("Deployed application on mesh"), logger);
    }
}