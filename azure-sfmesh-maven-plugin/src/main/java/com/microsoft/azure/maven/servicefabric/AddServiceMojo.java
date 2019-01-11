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
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Goal which adds a service resource to a project.
 */
@Mojo(name = "addservice", defaultPhase = LifecyclePhase.NONE)
public class AddServiceMojo extends AbstractMojo {

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
    @Parameter(property = "networkRef", alias = "networkName")
    String networkRef; 

    /**
     * Environmental variables suppiled in key1:val1,key2:val2 format
     */
    @Parameter(property = "environmentalVariables", defaultValue = Constants.DEFAULT_ENVIRONMENTAL_VARIABLES)
    String environmentalVariables;

    private Log logger  = getLog();

    @Override
    public void execute() throws MojoFailureException {
        addService();
    }
    
    public void addService() throws MojoFailureException{
        final String serviceFabricResourcesDirectory = Utils.getServicefabricResourceDirectory(logger, project);
        final String appResourcesDirectory = Utils.getAppResourcesDirectory(logger, project);
        final String serviceDirectory = Utils.getPath(serviceFabricResourcesDirectory, serviceName);
        if (!Utils.checkIfExists(serviceFabricResourcesDirectory)){
            throw new MojoFailureException("Service fabric resources folder does not exist." +
                "Please run init goal before running this goal!");
        }
        if (!Utils.checkIfExists(Utils.getPath(appResourcesDirectory, "app_" + applicationName + ".yaml"))){
            throw new MojoFailureException(String.format("Application resource" +
                "with the name %s does not exist", applicationName));
        }
        if (Utils.checkIfExists(serviceDirectory)){
            throw new MojoFailureException("Service Resource with the specified name already exists");
        }
        String serviceContent = new YamlContent.Builder()
                .addElement("SCHEMA_VERSION", schemaVersion)
                .addElement("APP_NAME", applicationName)
                .addElement("SERVICE_NAME", serviceName)
                .addElement("SERVICE_DESCRIPTION", serviceDescription)
                .addElement("OS_TYPE", getOS())
                .addElement("CODE_PACKAGE_NAME", getCodePackageName())
                .addElement("DOCKER_IMAGE", imageName)
                .addElement("LISTENER_NAME", getListenerName())
                .addElement("LISTENER_PORT", listenerPort)
                .addElement("CPU_USAGE", cpuUsage)
                .addElement("MEMORY_USAGE", memoryUsage)
                .addElement("REPLICA_COUNT", replicaCount)
                .addElement("NETWORK_NAME", networkRef)
                .build(logger, Constants.SERVICE_RESOURCE_NAME);
        try {
            if (!environmentalVariables.equals(Constants.DEFAULT_ENVIRONMENTAL_VARIABLES)){
                serviceContent = addEnvironmentVariables(logger, serviceContent, environmentalVariables);
            }
            Utils.createDirectory(logger, serviceDirectory);
            FileUtils.fileWrite(Utils.getPath(serviceDirectory,
                "service_" + serviceName + ".yaml"), serviceContent);
            logger.debug(String.format("Wrote %s service content to output", serviceName));
            TelemetryHelper.sendEvent(TelemetryEventType.ADDSERVICE,
                String.format("Added service with name: %s", serviceName), logger);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException("Error while writing output");
        }
    }

    @SuppressWarnings("unchecked")
    public String addEnvironmentVariables(Log logger, String content,
        String environmentVariables) throws MojoFailureException{
        final String[] env = environmentVariables.split(",");
        final ArrayList<LinkedHashMap<String, Object>> envList = new ArrayList<LinkedHashMap<String, Object>>();
        for (int i = 0; i < env.length; i++){
            final String[] kvp = env[i].split(":");
            final LinkedHashMap<String, Object> envMap = new LinkedHashMap<String, Object>();
            envMap.put(kvp[0], kvp[1]);
            envList.add(envMap);
        }
        final LinkedHashMap<String, Object> map = Utils.stringToYaml(logger, content);
        final LinkedHashMap<String, Object> application = (LinkedHashMap<String, Object>) map.get("application");
        final LinkedHashMap<String, Object> applicationProperties =
            (LinkedHashMap<String, Object>) application.get("properties");
        final ArrayList<LinkedHashMap<String, Object>> services =
            (ArrayList<LinkedHashMap<String, Object>>) applicationProperties.get("services");
        final LinkedHashMap<String, Object> service = services.get(0);
        final LinkedHashMap<String, Object> serviceProperties =
            (LinkedHashMap<String, Object>) service.get("properties");
        final ArrayList<LinkedHashMap<String, Object>> codePackages =
            (ArrayList<LinkedHashMap<String, Object>>) serviceProperties.get("codePackages");
        final LinkedHashMap<String, Object> codePackage = codePackages.get(0);

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

    String getOS(){
        if (osType.equals(Constants.DEFAULT_OS)){
            if (Utils.isLinux()){
                osType = Constants.LINUX_OS;
            } else {
                osType = Constants.WINDOWS_OS;
            }
        }
        return osType;
    }

    String getCodePackageName(){
        if (codePackageName.equals(Constants.DEFAULT_CODE_PACKAGE_NAME)){
            codePackageName = serviceName + "CodePackage";
        }
        return codePackageName;
    }

    String getListenerName(){
        if (listenerName.equals(Constants.DEFAULT_LISTENER_NAME)){
            listenerName = serviceName + "Listener";
        }
        return listenerName;
    }
}
