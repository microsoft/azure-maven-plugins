package com.microsoft.azure.maven.servicefabric;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.apache.maven.plugins.annotations.LifecyclePhase;

/**
 * Goal which adds a service resource to a project.
 */
@Mojo( name = "addservice", defaultPhase = LifecyclePhase.NONE )
public class AddServiceMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * schema version of the network yaml to be generated
    */
    @Parameter(property = "schemaVersion", defaultValue = Constants.DEFAULT_SCHEMA_VERSION)
    String schemaVersion;

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
     * Container image name of the service
    */
    @Parameter(property = "imageName", required = true)
    String imageName;

    /**
     * Description of the service
    */
    @Parameter(property = "serviceDescription", defaultValue = Constants.DEFAULT_SERVICE_DESCRIPTION)
    String serviceDescription;

    /**
     * OS environment on which this service is deployed
    */
    @Parameter(property = "osType", defaultValue = Constants.DEFAULT_OS)
    String osType;
    
    /**
     * Name of the code package
    */
    @Parameter(property = "codePackageName", defaultValue = Constants.DEFAULT_CODE_PACKAGE_NAME)
    String codePackageName;

    /**
     * Name of the listener
    */
    @Parameter(property = "listenerName", defaultValue = Constants.DEFAULT_LISTENER_NAME)
    String listenerName;
    
    /**
     * Port to expose of the container
    */
    @Parameter(property = "listenerPort")
    String listenerPort;

    /**
     * Max CPU usage (in cores) of the container
    */
    @Parameter(property = "cpuUsage", defaultValue = Constants.DEFAULT_CPU_USAGE)
    String cpuUsage;

    /**
     * Max Memory usage (in GB) of the container
    */
    @Parameter(property = "memoryUsage", defaultValue = Constants.DEFAULT_MEMORY_USAGE)
    String memoryUsage;

    /**
     * Replica count of the container
    */
    @Parameter(property = "replicaCount", defaultValue = Constants.DEFAULT_REPLICA_COUNT)
    String replicaCount;    

    /**
     * Network resource reference in which the container should be deployed
    */
    @Parameter(property = "networkRef", alias= "networkName")
    String networkRef; 

    /**
     * Enviromental variables suppiled in key1:val1,key2:val2 format
     */
    @Parameter(property = "enviromentalVariables", defaultValue = Constants.DEFAULT_ENVIRONMENTAL_VARIABLES)
    String enviromentalVariables;

    private Log logger  = getLog();

	@Override
	public void execute() throws MojoFailureException {
        addService();
    }
    
    public void addService() throws MojoFailureException{
        String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        String serviceDirectory = Utils.getPath(serviceFabricResourcesDirectory, serviceName);
        if(!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder does not exist. Please run init goal before running this goal!");
        }
        else{
            if(!Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "app_" + applicationName + ".yaml"))){
                throw new MojoFailureException(String.format("Application resource with the name %s does not exist", applicationName));
            }
            if(Utils.checkIfExists(serviceDirectory)){
                throw new MojoFailureException("Service Resource with the specified name already exists");
            }
            try {
                InputStream resource = this.getClass().getClassLoader().getResourceAsStream(Constants.SERVICE_RESOURCE_NAME);
                String serviceContent = IOUtil.toString(resource, "UTF-8"); 
                serviceContent = Utils.replaceString(logger, serviceContent, "SCHEMA_VERSION", schemaVersion, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "APP_NAME", applicationName, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "SERVICE_NAME", serviceName, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "SERVICE_DESCRIPTION", serviceDescription, Constants.SERVICE_RESOURCE_NAME);
                if(osType.equals(Constants.DEFAULT_OS)){
                    if(Utils.isLinux()){
                        osType = Constants.LINUX_OS;
                    }
                    else{
                        osType = Constants.WINDOWS_OS;
                    }
                }
                serviceContent = Utils.replaceString(logger, serviceContent, "OS_TYPE", osType, Constants.SERVICE_RESOURCE_NAME);
                if(codePackageName.equals(Constants.DEFAULT_CODE_PACKAGE_NAME)){
                    codePackageName = serviceName + "CodePackage";
                }
                serviceContent = Utils.replaceString(logger, serviceContent, "CODE_PACKAGE_NAME", codePackageName, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "DOCKER_IMAGE", imageName, Constants.SERVICE_RESOURCE_NAME);
                if(listenerName.equals(Constants.DEFAULT_LISTENER_NAME)){
                    listenerName = serviceName + "Listener";
                }
                serviceContent = Utils.replaceString(logger, serviceContent, "LISTENER_NAME", listenerName, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "LISTENER_PORT", listenerPort, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "CPU_USAGE", cpuUsage, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "MEMORY_USAGE", memoryUsage, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "REPLICA_COUNT", replicaCount, Constants.SERVICE_RESOURCE_NAME);
                serviceContent = Utils.replaceString(logger, serviceContent, "NETWORK_NAME", networkRef, Constants.SERVICE_RESOURCE_NAME);

                if(!enviromentalVariables.equals(Constants.DEFAULT_ENVIRONMENTAL_VARIABLES)){
                    serviceContent = AddEnvironmentVariables(logger, serviceContent, enviromentalVariables);
                }
                Utils.createDirectory(logger, serviceDirectory);
                FileUtils.fileWrite(Utils.getPath(serviceDirectory, "service_" + serviceName + ".yaml"), serviceContent);
                logger.debug(String.format("Wrote %s service content to output", serviceName));
                TelemetryHelper.sendEvent(TelemetryEventType.ADDSERVICE, String.format("Added service with name: %s", serviceName), logger);
            } catch (IOException e) {
                logger.error(e);
                throw new MojoFailureException("Error while writing output");
            }
    
        }
    }

    @SuppressWarnings("unchecked")
    public String AddEnvironmentVariables(Log logger, String content, String environmentVariables) throws MojoFailureException{
        String[] env = environmentVariables.split(",");
        ArrayList<LinkedHashMap<String, Object>> envList = new ArrayList<LinkedHashMap<String, Object>>();
        for(int i=0; i<env.length; i++){
            String[] kvp = env[i].split(":");
            LinkedHashMap<String, Object> envMap = new LinkedHashMap<String, Object>();
            envMap.put(kvp[0], kvp[1]);
            envList.add(envMap);
        }
        LinkedHashMap<String, Object> map = Utils.stringToYaml(logger, content);
        LinkedHashMap<String, Object> application = (LinkedHashMap<String, Object>)map.get("application");
        LinkedHashMap<String, Object> applicationProperties = (LinkedHashMap<String, Object>)application.get("properties");
        ArrayList<LinkedHashMap<String, Object>> services = (ArrayList<LinkedHashMap<String, Object>>)applicationProperties.get("services");
        LinkedHashMap<String, Object> service = services.get(0);
        LinkedHashMap<String, Object> serviceProperties = (LinkedHashMap<String, Object>)service.get("properties");
        ArrayList<LinkedHashMap<String, Object>> codePackages = (ArrayList<LinkedHashMap<String, Object>>)serviceProperties.get("codePackages");
        LinkedHashMap<String, Object> codePackage = codePackages.get(0);

        // TODO: Understand the reason why we need to remove instead of replace
        codePackage.put("environmentVariables", envList);
        codePackages.remove(0);
        codePackages.add(codePackage);
        serviceProperties.replace("codePackages", codePackages);
        service.replace("properties", serviceProperties);
        services.remove(0);
        services.add(0, service);
        applicationProperties.replace("properties", services);
        application.replace("application", applicationProperties);
        map.replace("application", application);
        return Utils.yamlToString(map);
    }
}