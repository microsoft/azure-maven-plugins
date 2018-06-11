# Maven Plugin for Azure App Service
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22)

#### Table of Content
- [Prerequisites](#prerequisites)
- [Goals](#goals)
- [Authentication with Azure](#authentication-with-azure)
- [Quick Start](#quick-start)
- [Azure App Service](#azure-app-service)
    - [Web App (on Windows)](#web-app-on-windows)
    - [Web App (on Linux)](#web-app-on-linux)
    - [Web App for Containers](#web-app-for-containers)
 - [Deployment Type](#deployment-type)


The Maven Plugin for Azure App Service Web Apps provides seamless integration into Maven projects, 
and makes it easier for developers to deploy to Web App (on Windows) and [Web App on Linux](https://docs.microsoft.com/azure/app-service-web/app-service-linux-intro) in Azure.

## Prerequisites

Tool | Required Version
---|---
JDK | 1.7 and above
Maven | 3.0 and above

<a name="goals"></a>
## Goals

The Maven Plugin for Azure App Service Web Apps has only one goal: `azure-webapp:deploy`. 

Goal | Description
--- | ---
`azure-webapp:deploy` | Deploy artifacts or docker container image to an Azure Web App based on your configuration.<br>If the specified Web App does not exist, it will be created.

## Authentication with Azure

You can use the Azure CLI 2.0 for authenticatin. More authentication methods can be found at [this link](../docs/common-configuration.md).  

1. Install the Azure CLI 2.0 by following the instructions in the [Install Azure CLI 2.0](https://docs.microsoft.com/cli/azure/install-azure-cli) article.

2. Run the following commands to log into your Azure subscription:

   ```shell
   az login
   az account set --subscription <subscription Id>
   ```

## Quick Start 

1. Create a new Azure App Service and choose Linux with built-in tomcat as the environment. 

2. To use this plugin in your Maven project, add the following settings for the plugin to your `pom.xml` file:

   ```xml
   <project>
      ...
      <build>
         <pluginManagement>
            <plugins>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <!-- check Maven Central for the latest version -->
               <version>1.1.0</version>
            </plugins>
         </pluginManagement>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <configuration>
                  
               <!-- Web App information -->
               <resourceGroup>your-resource-group</resourceGroup>
               <appName>your-app-name</appName>
                
                <!-- Java Runtime Stack for Web App on Linux-->
               <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
    
               <!-- WAR deployment -->
               <deploymentType>war</deploymentType>
    
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   <project>
   ```
   
3. Use the following commands to deploy your project to Azure App Service. 

   ```azure-webapp:deploy```

## Azure App Service 
A few typical usages of Maven Plugin for Azure App Service Web Apps are listed at [Web App Samples](../docs/web-app-samples.md).
You can choose one to quickly get started.

### Web App (on Windows)

For Web App (on Windows), only Java runtime stack is supported in our plugin.
You can use `<javaVersion>` and `<javaWebContainer>` to configure the runtime of your Web App.

#### Java Runtime
Use values from the following table to configure the JVM you want to use in your Web App.

Supported Value | Description
---|---
`1.7` | Java 7, Newest minor version
`1.7.0_51` | Java 7, Update 51
`1.7.0_71` | Java 7, Update 71
`1.8` | Java 8, Newest minor version
`1.8.0_25` | Java 8, Update 25
`1.8.0_60` | Java 8, Update 60
`1.8.0_73` | Java 8, Update 73
`1.8.0_111` | Java 8, Update 111
`1.8.0_92` | Azul's Zulu OpenJDK 8, Update 92
`1.8.0_102` | Azul's Zulu OpenJDK 8, Update 102

It is recommended to ignore the minor version number like the following example, so that the latest supported JVM will be used in your Web App.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>azure-webapp-maven-plugin</artifactId>
      <configuration>
         <javaVersion>1.8</javaVersion>
         ...
      </configuration>
   </plugin>
   ```

#### Web Container

Use values from the following table to configure the Web Container in your Web App.

Supported Value | Description
---|---
`tomcat 7.0` | Newest Tomcat 7.0
`tomcat 7.0.50` | Tomcat 7.0.50
`tomcat 7.0.62` | Tomcat 7.0.62
`tomcat 8.0` | Newest Tomcat 8.0
`tomcat 8.0.23` | Tomcat 8.0.23
`tomcat 8.5` | Newest Tomcat 8.5
`tomcat 8.5.6` | Tomcat 8.5.6
`jetty 9.1` | Newest Jetty 9.1
`jetty 9.1.0.20131115` | Jetty 9.1.0.v20131115
`jetty 9.3` | Newest Jetty 9.3
`jetty 9.3.13.20161014` | Jetty 9.3.13.v20161014

It is recommended to ignore the minor version number like the following example, so that the latest supported web container will be used in your Web App.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>azure-webapp-maven-plugin</artifactId>
      <configuration>
         <javaWebContainer>tomcat 8.5</javaWebContainer>
         ...
      </configuration>
   </plugin>
   ```

### Web App (on Linux)

#### Java Runtime and Web Container
Use values from the following table to configure the JVM and Web Container you want to use in your Web App.

Supported Value | Description
---|---
`tomcat 8.5-jre8` | Java 8, Tomcat 8.5
`tomcat 9.0-jre8` | Java 8, Tomcat 9.0

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>azure-webapp-maven-plugin</artifactId>
      <configuration>
         <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
         ...
      </configuration>
   </plugin>
   ```

### Web App for Containers

#### Container Setting

In the `<containerSettings>` element of your `pom.xml` file, you can sepcify which docker container image to deploy to your Web App. Typically, this image should be from a private container registry which is built from your app, but you can also use images from a docker hub.

You can specify the following properties within the `<containerSettings>` element:

Property | Required | Description
---|---|---
`<imageName>` | true | Specifies the Docker image name. Valid image name formats are listed as below.<br>- Docker Hub image: `[hub-user/]repo-name[:tag]`; `tag` is optional, default value is **latest**.<br>- Private registry image: `hostname/repo-name[:tag]`; `tag` is optional, default value is **latest**.
`<serverId>` | false | Specifies the credentials for private docker hub images or private container registry images. (Note: `serverId` should be from your Maven `setting.xml` file.)
`<registryUrl>` | false | Specifies the URL of private container registry images.

Check out samples at [Web App Samples](../docs/web-app-samples.md) for the configuration settings for different image sources.


## Deployment Type 

#### Deploy via FTP
You can deploy your **WAR** file and other artifacts to Web App via FTP. The following example shows all configuration elements.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>azure-webapp-maven-plugin</artifactId>
      <configuration>
         <deploymentType>ftp</deploymentType>
         <resources>
            <resource>
                <directory>${project.basedir}/target</directory>
                <targetPath>webapps</targetPath>
                <includes>
                    <include>*.war</include>
                </includes>
                <excludes>
                    <exclude>*.xml</exclude>
                </excludes>
            </resource>
         </resources>
         ...
      </configuration>
   </plugin>
   ```
   
   Detailed explanation of the `<resource>` element is listed in the following table.
   
   Property | Description
   ---|---
   `directory` | Specifies the absolute path where the resources are stored.
   `targetPath` | Specifies the target path where the resources will be deployed to.<br>This is a relative path to the `/site/wwwroot/` folder of FTP server in your Web App.
   `includes` | A list of patterns to include, e.g. `**/*.xml`.
   `excludes` | A list of patterns to exclude, e.g. `**/*.xml`.

