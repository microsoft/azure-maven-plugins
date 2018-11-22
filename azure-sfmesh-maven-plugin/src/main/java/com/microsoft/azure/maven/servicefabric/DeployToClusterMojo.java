package com.microsoft.azure.maven.servicefabric;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugin.logging.Log;
/**
 * Goal which deploys the application to a cluster
 */
@Mojo( name = "deploytocluster", defaultPhase = LifecyclePhase.NONE )
public class DeployToClusterMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Comma seperated resource files or the directory in which the resource files are present
    */
    @Parameter(property = "inputYamlFilePaths", defaultValue = Constants.SERVICE_FABRIC_RESOURCES_PATH)
    String inputYamlFilePaths;

    /**
     * URL of the cluster in which this application should be deployed.
    */
    @Parameter(property = "clusterEndpoint", defaultValue = Constants.DEFAULT_CLUSTER_ENDPOINT)
    String clusterEndpoint;

    /**
     * Location of pem file.
     */
    @Parameter(property = "pemFilePath", defaultValue = Constants.DEFAULT_PEM_FILE_PATH)
    String pemFilePath;

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
        Utils.checksfctlinstallation(logger);
        if(pemFilePath.equalsIgnoreCase(Constants.DEFAULT_PEM_FILE_PATH)){
            Utils.connecttounsecurecluster(logger, clusterEndpoint);
            Utils.executeCommand(logger, "sfctl mesh deployment create --input-yaml-file-paths " + inputYamlFilePaths);
            TelemetryHelper.sendEvent(TelemetryEventType.DEPLOYLOCAL, String.format("Deployed application locally"), logger);
        }
        else{
            Utils.connecttosecurecluster(logger, clusterEndpoint, pemFilePath);
            Utils.executeCommand(logger, "sfctl mesh deployment create --input-yaml-file-paths " + inputYamlFilePaths);
            TelemetryHelper.sendEvent(TelemetryEventType.DEPLOYSFRP, String.format("Deployed application to SFRP"), logger);
        }
    }
}