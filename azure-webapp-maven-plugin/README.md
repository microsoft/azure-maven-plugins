# Maven Plugin for Azure App Service
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22)

The Maven Plugin for Azure App Service provides seamless integration into Maven projects, 
and makes it easier for developers to deploy to different kinds of Azure Web Apps:
  - [Web App on Windows](https://docs.microsoft.com/en-us/azure/app-service/app-service-web-overview)
  - [Web App on Linux](https://docs.microsoft.com/azure/app-service-web/app-service-linux-intro)
  - [Web App for Containers](https://docs.microsoft.com/en-us/azure/app-service/containers/tutorial-custom-docker-image)
  
#### Table of Content
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Samples](#samples)
- [Goals](#goals)
- [Authentication with Azure](#authentication-with-azure)
- [Configurations](#configurations)

## Prerequisites

Tool | Required Version
---|---
JDK | 1.7 or above
Maven | 3.0 or above
## Quick Start 

1. Create a new Azure App Service and choose Linux with built-in tomcat as the environment. 

2. To use this plugin in your Maven project, add the following settings for the plugin to your `pom.xml` file:

    ```xml
    <project>
       ...
       <packaging>war</packaging>
       ...
       <build>
          <pluginManagement>
             <plugins>
                <groupId>com.microsoft.azure</groupId>
                <artifactId>azure-webapp-maven-plugin</artifactId>
                <!-- check Maven Central for the latest version -->
                <version>1.3.0</version>
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
                </configuration>
             </plugin>
             ...
          </plugins>
       </build>
    </project>
    ```
   
3. Use the following commands to deploy your project to Azure App Service. 

    ```
    $ mvn azure-webapp:deploy
    ```

<a name="samples"></a>
## Samples	
A few typical usages of Maven Plugin for Azure App Service Web Apps are listed at [Web App Samples](../docs/web-app-samples.md). You can choose one to quickly get started.

<a name="goals"></a>
## Goals

The Maven Plugin for Azure App Service Web Apps has only one goal: `azure-webapp:deploy`. 

Goal | Description
--- | ---
`azure-webapp:deploy` | Deploy artifacts or docker container image to an Azure Web App based on your configuration.<br>If the specified Web App does not exist, it will be created.
## Authentication with Azure

You can use the Azure CLI 2.0 for authentication. More authentication methods can be found [here](../docs/common-configuration.md).  

1. Install the Azure CLI 2.0 by following the instructions in the [Install Azure CLI 2.0](https://docs.microsoft.com/cli/azure/install-azure-cli) article.

2. Run the following commands to log into your Azure subscription:

    ```
    $ az login
    $ az account set --subscription <subscription Id>
    ```
## Configurations

Common configurations of all Maven Plugins for Azure can be found [here](../docs/common-configuration.md).

The maven plugin supports two kinds of configurations V1 (default) and V2. Specify the configuration 
`<schemaVersion>V2</schemaVersion>` to use the V2 configuration.
The configurations of the region, the Web App runtime and the deployment are different in V1 and V2.
The common basic settings of the configuration are listed in the following table.

Property | Required | Description | Version
---|---|---|---
`<resourceGroup>` | true | Azure Resource Group for your Web App. | 0.1.0+
`<appName>` | true | The name of your Web App. | 0.1.0+
`<pricingTier>`* | false | The pricing tier for your Web App. The default value is **S1**.| 0.1.0+
`<deploymentSlot>` | false | The deployment slot to deploy your application. | 1.3.0+
`<appServicePlanResourceGroup>` | false | The resource group of the existing App Service Plan. If not specified, the value defined in `<resourceGroup>` will be used by default. | 1.0.0+
`<appServicePlanName>` | false | The name of the existing App Service Plan. | 1.0.0+
`<appSettings>` | false | Specifies the application settings for your Web App. | 0.1.0+
`<stopAppDuringDeployment>` | false | To stop the target Web App or not during deployment. This will prevent deployment failure caused by IIS locking files. | 0.1.4+

### V1 Configuration
1. Region

    The configuration `<region>` is optional in V1 configuration, and the default value is **westeurope**.

2. Runtime settings

    Details about the supported values of could be found in the following.
    tomcat 8.5 will be used as default value for `<javaWebContainer>`.
  
    **Web App on Windows**
    ```xml
    <configuration>
    ...
      <javaVersion>1.8</javaVersion>
      <javaWebContainer>tomcat 8.5</javaWebContainer>
    </configuration>
    ```
    **Web App on Linux**
    ```xml
    <configuration>
    ...
      <linuxRuntime>tomcat 8.5-jre8</javaVersion>
    </configuration>
    ```
    The supported values are *tomcat 8.5-jre8*, *tomcat 9.0-jre8*, *wildfly 14-jre8*, and *jre8*.

    **Web App for Containers**
    
    ```xml
    <configuration>
    ...
      <containerSettings>
       <!-- only the imageName is required --> 
        <imageName>[hub-user/]repo-name[:tag]</imageName>
        <serverId></serverId>
        <registryUrl></registryUrl>
      </containerSettings>
    </configuration>
    ```
    `tag` is optional for the `imageName`. 
    The default value is latest.
    Public docker hubs and private container 
    registries are both supported.
 
3. Deployment settings

    There are multiple deployment types are supported:
    
    - ZIP Deploy
    
    ZIP deploy is intended for fast and easy deployments.
    ```xml
    <configuration>
    ...
      <deploymentType>zip</deploymentType>
      <resources>
        <resource>
          <directory>${project.basedir}/target</directory>
          <includes>
          <include>*.jar</include>
          </includes>
          <excludes>
          <exclude>*.xml</exclude>
          </excludes>
        </resource>
      </resources>
    </configuration>
    ```
    - WAR Deploy
    
    Both `<warFile>` and `<path>` are optional. By default it will find the war file according to the `<finalName>` in 
    the  project build directory, and deploy to ROOT.
    ```xml
    <configuration>
    ...
      <deploymentType>war</deploymentType>
      <warFile></warFile>
      <path></path>
    </configuration>
    ```
    - JAR Deploy
    
    `<jarFile>` is not required. If not specified, it will deploy the `${project.build.directory}/${project.build.finalName}.jar` to `%HOME%\site\wwwroot\` of your Web App.
    > Please note that for Windows Web App, we will generate a `web.config` file, you can find more details [here](.
    ./docs/web-config.md).
    ```xml
    <configuration>
    ...
      <deploymentType>jar</deploymentType>
      <jarFile></jarFile>
    </configuration>
    ```
    
    - AUTO Deploy

    This is the default deployment type used by the plugin. It will inspect `<packaging>` field in the pom file to decide how to deploy the artifact. If the `<packaging>` is set to `war`, the plugin will use war deployment. If the `<packaging>` is set to `jar`, the plugin will use jar deployment.
    Otherwise, the plugin will skip the deployment, which is the same as `NONE` deployment.
    
    If you want the plugin to inspect the `<packaging>` field. Just don't set `<deploymentType>`. The plugin will use `AUTO` deployment as default.
    
    - ~~FTP~~ (deprecated)
    
    You can deploy your artifacts/resources to Web App via FTP.
    ```xml
    <configuration>
    ...
      <deploymentType>ftp</deploymentType>
      <resources>
        <resource>
          <directory>${project.basedir}/target</directory>
          <includes>
          <include>*.jar</include>
          </includes>
          <excludes>
          <exclude>*.xml</exclude>
          </excludes>
        </resource>
      </resources>
    </configuration>
    ```
    
### V2 Configuration

1. Region

    The configuration `<region>` is required in V2 configuration, supported values could be found in the following.

2. Runtime settings

    Supported `<os>` values are *Windows*, *Linux* and *Docker*.
    Only the `jre8` is supported for `<javaVersion>` for Web App on Linux.
    If the `<webContainer>` is not configured and the `<os>` is windows, tomcat 8.5 will be used as default value.
    But if the `<os>` is linux, the web app will use the JavaSE as runtime.
    
    The runtime settings of v2 configuration could be omitted if users specify an existing web app in the configuration and just want to do the deploy directly.
    ```xml
    <configuration>
    ...
    <runtime>
      <os>Linux</os>
      <javaVersion>jre8</javaVersion>
      <webContainer></webContainer>
    </runtime>
    <!-- os -->
    <runtime>
      <os>Docker</os>
      <!-- only image is required -->
      <image>[hub-user/]repo-name[:tag]</image>
      <serverId></serverId>
      <registryUrl></registryUrl>
     </runtime>
    </configuration>
    ```

3. Deployment settings

    Users don't need to care about the deployment type in v2 configuration.
    Just configure the resources to deploy to the Web App.
    
    It will use the zip deploy for fast and easy deploy.
    But if the artifact(s) are war package(s), war deploy will be used.
    Mix deploying war packages and other kinds of artifacts is not suggested and will cause errors.
    ```xml
    <configuration>
    ...
    <deployment>
     <resources>
       <resource>
         <directory>${project.basedir}/target</directory>
         <includes>
         <include>*.jar</include>
         </includes>
         <excludes>
         <exclude>*.xml</exclude>
         </excludes>
       </resource>
      </resources>
    </deployment>
    </configuration>
    ```
### Details explanation of some configurations
#### `<javaVersion>`
The supported values for Web App on Linux is only **jre8**.

The supported values for Web App on Windows:

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
> Note: It is recommended to ignore the minor version number so that the latest supported JVM will be used in your Web App.

#### `<javaWebContainer>` or `<webContainer>`
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
`wildfly 14` | WildFly 14
> Note: It is recommended to ignore the minor version number so that the latest supported web container will be used in your Web App.

#### `<resource>`
Property | Description
---|---
`directory` | Specifies where the resources are stored.
`targetPath` | Specifies the target path where the resources will be deployed to.
`includes` | A list of patterns to include, e.g. `**/*.war`.
`excludes` | A list of patterns to exclude, e.g. `**/*.xml`.

> Note: The `<targetPath>` is relative to the `/site/wwwroot/` folder except one case: it is relative to the 
`/site/wwwroot/webapps` when you deploy the war package.

#### `<region>`
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

#### `<pricingTier>`
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
