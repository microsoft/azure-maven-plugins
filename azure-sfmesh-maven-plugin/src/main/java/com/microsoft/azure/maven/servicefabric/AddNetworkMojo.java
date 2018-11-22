package com.microsoft.azure.maven.servicefabric;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;


/**
 * Goal which adds a network resource to a project.
 */
@Mojo(name = "addnetwork", defaultPhase = LifecyclePhase.NONE)
public class AddNetworkMojo extends AbstractMojo
{
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
    @Parameter(property = "networkDescription", defaultValue= Constants.DEFAULT_NETWORK_DESCRIPTION)
    String networkDescription;

    /**
     * Kind of the network
     */
    @Parameter(property = "networkKind", defaultValue= Constants.DEFAULT_NETWORK_KIND)
    String networkKind;

    /**
     * Address prefix of the subnet
     */
    @Parameter(property = "networkAddressPrefix", required = true)
    String networkAddressPrefix;

    public Log logger  = getLog();
	
	@Override
	public void execute() throws MojoFailureException {
        String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
		String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if(!Utils.checkIfExists(serviceFabricResourcesDirectory)){
        	throw new MojoFailureException("Service fabric resources folder does not exist. Please run init goal before running this goal!");
        }
        else{
            if(Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "network_" + networkName + ".yaml"))){
                throw new MojoFailureException("Network Resource with the specified name already exists");
            }
            InputStream resource = this.getClass().getClassLoader().getResourceAsStream(Constants.NETWORK_RESOURCE_NAME);
            try {
                String networkContent = IOUtil.toString(resource, "UTF-8");
                networkContent = Utils.replaceString(logger, networkContent, "SCHEMA_VERSION", schemaVersion, Constants.NETWORK_RESOURCE_NAME);
                networkContent = Utils.replaceString(logger, networkContent, "NETWORK_NAME", networkName, Constants.NETWORK_RESOURCE_NAME);
                networkContent = Utils.replaceString(logger, networkContent, "NETWORK_DESCRIPTION", networkDescription, Constants.NETWORK_RESOURCE_NAME);
                networkContent = Utils.replaceString(logger, networkContent, "ADDRESS_PREFIX", networkAddressPrefix, Constants.NETWORK_RESOURCE_NAME);
                networkContent = Utils.replaceString(logger, networkContent, "NETWORK_KIND", networkKind, Constants.NETWORK_RESOURCE_NAME);
                FileUtils.fileWrite(Utils.getPath(appResourcesDirectory, "network_" + networkName + ".yaml"), networkContent);
				logger.debug(String.format("Wrote %s network content to output", networkName));
                TelemetryHelper.sendEvent(TelemetryEventType.ADDNETWORK, String.format("Added network with name: %s", networkName), logger);
            }
            catch (IOException e) {
				logger.error(e);
				throw new MojoFailureException("Error while writing output");
			} 
        }
    }
}