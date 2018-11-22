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
 * Goal which adds a secret value resource to a project.
 */
@Mojo(name = "addsecretvalue", defaultPhase = LifecyclePhase.NONE)
public class AddSecretValueMojo extends AbstractMojo
{
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


    public Log logger  = getLog();
	
	@Override
	public void execute() throws MojoFailureException {
        String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
		String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        if(!Utils.checkIfExists(serviceFabricResourcesDirectory)){
        	throw new MojoFailureException("Service fabric resources folder does not exist. Please run init goal before running this goal!");
        }
        else{
            String[] secretValueSplit = secretValueName.split("/", 2);
            if(Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "secretvalue_" + secretValueSplit[0] +"_"+ secretValueSplit[1]+ ".yaml"))){
                throw new MojoFailureException("Secret Value Resource with the specified name already exists");
            }
            InputStream resource = this.getClass().getClassLoader().getResourceAsStream(Constants.SECRET_VALUE_RESOURCE_NAME);
            try {
                String secretValueContent = IOUtil.toString(resource, "UTF-8");
                secretValueContent = Utils.replaceString(logger, secretValueContent, "SCHEMA_VERSION", schemaVersion, Constants.SECRET_VALUE_RESOURCE_NAME);
                secretValueContent = Utils.replaceString(logger, secretValueContent, "SECRET_VALUE_NAME", secretValueName, Constants.SECRET_VALUE_RESOURCE_NAME);
                secretValueContent = Utils.replaceString(logger, secretValueContent, "SECRET_VALUE", secretValue, Constants.SECRET_VALUE_RESOURCE_NAME);
                FileUtils.fileWrite(Utils.getPath(appResourcesDirectory, "secretvalue_" + secretValueSplit[0] +"_"+ secretValueSplit[1]+ ".yaml"), secretValueContent);
				logger.debug(String.format("Wrote %s secret value content to output", secretValueName));
                TelemetryHelper.sendEvent(TelemetryEventType.ADDSECRETVALUE, String.format("Added secret value with name: %s", secretValueName), logger);
            }
            catch (IOException e) {
				logger.error(e);
				throw new MojoFailureException("Error while writing output");
			} 
        }
    }
}