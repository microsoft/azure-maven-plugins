# Maven Plugin for Azure Web Apps
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22)

#### Table of Content
- [Prerequisites](#prerequisites)
- [Goals](#goals)
- [Usage](#usage)
- [Quick Samples](#quick-samples)
- [Common Configuration](#common-configuration)
- [Configuration](#configuration)
    - [Web App (on Windows)](#web-app-on-windows)
        - [Java Runtime](#java-runtime)
        - [Web Container](#web-container)
    - [Web App (on Linux)](#web-app-on-linux)
        - [Java Runtime and Web Container](#java-runtime-and-web-container)
    - [Web App for Containers](#web-app-for-containers)
        - [Container Setting](#container-setting)
    - [Supported Regions](#supported-regions)
    - [Supported Pricing Tiers](#supported-pricing-tiers)


The Maven Plugin for Azure Web Apps provides seamless integration of Azure Web Apps into Maven, 
and makes it easier for developers to deploy to Web App (on Windows) and [Web App on Linux](https://docs.microsoft.com/azure/app-service-web/app-service-linux-intro) in Azure.

**Note**: This plugin is still in preview; feedback and feature requests are warmly welcome.

## Prerequisites

Tool | Required Version
---|---
JDK | 1.7 and above
Maven | 3.0 and above

<a name="goals"></a>
## Goals

The Maven Plugin for Azure Web Apps has only one goal: `azure-webapp:deploy`. 

Goal | Description
--- | ---
`azure-webapp:deploy` | Deploy artifacts or docker container image to an Azure Web App based on your configuration.<br>If the specified Web App does not exist, it will be created.

## Usage

To use the Maven Plugin for Azure Web Apps in your Maven Java app, add the following settings for the plugin to your `pom.xml` file:

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.1.0</version>
               <configuration>
                  ...
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   <project>
   ```

## Quick Samples
A few typical usages of Maven Plugin for Azure Web Apps are listed at [Web App Samples](../docs/web-app-samples.md).
You can choose one to quickly get started.

## Common Configuration

This Maven plugin supports common configurations of all Maven Plugins for Azure.
Detailed documentation of common configurations is at [here](../docs/common-configuration.md).

## Configuration

This Maven Plugin supports the following configuration properties:

Property | Required | Description | Version
---|---|---|---
`<resourceGroup>` | true | Specifies the Azure Resource Group for your Web App. | 0.1.0+
`<appName>` | true | Specifies the name of your Web App. | 0.1.0+
`<region>`* | false | Specifies the region where your Web App will be hosted; the default value is **westus**. All valid regions at [Supported Regions](#supported-regions) section. | 0.1.0+
`<pricingTier>`* | false | Specifies the pricing tier for your Web App; the default value is **S1**. All valid tiers are at [Supported Pricing Tiers](#supported-pricing-tiers) section. | 0.1.0+
`<appServicePlanResourceGroup>` | false | Specifies the resource group of the existing App Service Plan when you do not want to create a new one. If this setting is not specified, plugin will use the value defined in `<resourceGroup>`. | 1.0.0+
`<appServicePlanName>` | false | Specifies the name of the existing App Service Plan when you do not want to create a new one. | 1.0.0+
`<javaVersion>` | false | Specifies the JVM version for your Web App.<br>This setting is only applicable for Web App (on Windows); see the [Java Runtime](#java-runtime) section of this README for details. | 0.1.0+
`<javaWebContainer>` | false | Specified the Web Container for your Web App.<br>This setting is only applicable for Web App (on Windows); see the [Web Container](#web-container) section of this README for details. | 0.1.0+
`<linuxRuntime>` | false | Specified the runtime stack for your Web App.<br>This setting is only applicable for Web App (on Linux); see the [Java Runtime and Web Container](#java-runtime-and-web-container) section of this README for details. | 0.2.0+
`<containerSettings>` | false | Specifies the docker container image to deploy to your Web App.<br>This setting is only applicable for Web App for Containers. Docker hubs and private container registries are both supported; see the [Container Setting](#container-setting) section of this README for details. | 0.1.0+
`<appSettings>` | false | Specifies the application settings for your Web App, which are defined in name-value pairs like following example:<br>`<property>`<br>&nbsp;&nbsp;&nbsp;&nbsp;`<name>xxxx</name>`<br>&nbsp;&nbsp;&nbsp;&nbsp;`<value>xxxx</value>`<br>`</property>` | 0.1.0+
`<stopAppDuringDeployment>` | false | Specifies whether stop target Web App during deployment. This will prevent deployment failure caused by IIS locking files. | 0.1.4+
`<deploymentType>` | false | Specifies the deployment approach you want to use, available options: <br><ul><li>war (by default): since 1.1.0</li><li>ftp: since 0.1.0</li></ul> | 0.1.0+
`<resources>` | false | Specifies the artifacts to be deployed to your Web App when `<deploymentType>` is set to `ftp`; see the [Deploy via FTP](#deploy-via-ftp) section for more details. | 0.1.0+
`<warFile>` | false | Specifies the location of the war file which is to be deployed when `<deploymentType>` is set to `war`. If this configuration is not specified, plugin will find the war file according to the `finalName` in the project build directory. | 1.1.0+
`<path>` | false | Specify the context path for the deployment when `<deploymentType>` is set to `war`. If this configuration is not specified, plugin will deploy to the context path: `/`, which is also known as the `ROOT`. | 1.1.0+
>*: This setting will be used only when you are creating a new Web App; if the Web App already exists, this setting will be ignored

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

#### Deploy via FTP
You can deploy your **WAR** file and other artifacts to Web App via FTP. Please refer to [the example in previous section](#deploy-via-ftp) for all configuration elements.

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

### Supported Regions
All valid regions are listed as below. Read more at [Azure Region Availability](https://azure.microsoft.com/en-us/regions/services/).
- `westus`
- `westus2`
- `centralus`
- `eastus`
- `eastus2`
- `northcentralus`
- `southcentralus`
- `westcentralus`
- `canadacentral`
- `canadaeast`
- `brazilsouth`
- `northeurope`
- `westeurope`
- `uksouth`
- `ukwest`
- `eastasia`
- `southeastasia`
- `japaneast`
- `japanwest`
- `australiaeast`
- `australiasoutheast`
- `centralindia`
- `southindia`
- `westindia`
- `koreacentral`
- `koreasouth`

### Supported Pricing Tiers
All valid pricing tiers are listed as below. Read more at [Azure App Service Plan Pricing](https://azure.microsoft.com/en-us/pricing/details/app-service/).
- `F1`
- `D1`
- `B1`
- `B2`
- `B3`
- `S1`
- `S2`
- `S3`
- `P1`
- `P2`
- `P3`
