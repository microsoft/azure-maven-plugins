# Azure Web Apps Maven Plugin

The Azure Web Apps plugin provides seamless integration of Azure Web Apps into Maven, 
and makes it easier for developers to deploy to Web App and [Web App on Linux](https://docs.microsoft.com/azure/app-service-web/app-service-linux-intro) in Azure.

**Note**: This plugin is still in preview; feedback and feature requests are warmly welcome.

## Prerequisites

Tool | Required Version
---|---
JDK | 1.7 and above
Maven | 3.0 and above

## Goals

The Azure Web Apps plugin has only one goal: `webapp:deploy`, which is bounded to the `deploy` phase. 

Goal | Description
--- | ---
`webapp:deploy` | Deploy artifacts or docker container image to an Azure Web App based on your configuration.<br>If the specified Web App does not exist, it will be created.

## Usage

To use the Azure Web Apps plugin in your Maven Java app, add the following settings for the plugin to your `pom.xml` file:

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>webapp-maven-plugin</artifactId>
               <version>0.1.0</version>
               <configuration>
                  ...
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   <project>
   ```

## Configuration

The Azure Web Apps plugin for Maven supports the following configuration properties:

Property | Required | Description
---|---|---
`<authentication>`| false | Specifies which authentication method to use with Azure.<br>There are three supported methods, which are described in the [Authentication with Azure](#authentication-with-azure) section of this README.
`<subscriptionId>` | false | Specifies the target subscription.<br>Use this setting when you have multiple subscriptions in your authentication file.
`<resourceGroup>` | true | Specifies the Azure Resource Group for your Web App.
`<appName>` | true | Specifies the name of your Web App.
`<region>` | false | Specifies the region where your Web App will be hosted; the default value is **westus**.<br>This setting will be used only when you are creating a new Web App; if the Web App already exists, this setting will be ignored.
`<pricingTier>` | false | Specifies the pricing tier for your Web App; the default value is **S1**.<br>This setting will be used only when you are creating a new Web App; if the Web App already exists, this setting will be ignored.
`<javaVersion>` | false | Specifies the JVM version for your Web App.<br>This setting is only applicable for normal Web App (Windows based); see the [Java Runtime](#java-runtime) section of this README for details.
`<javaWebContainer>` | false | Specified the Web Container for your Web App.<br>This setting is only applicable for normal Web App (Windows based); see the [Web Container](#web-container) section of this README for details.
`<containerSettings>` | false | Specifies the docker container image to deploy to your Web App.<br>Docker hubs and private container registries are both supported; see the [Container Setting](#container-setting) section of this README for details.
`<appSettings>` | false | Specifies the application settings for your Web App, which are defined in name-value pairs like following example:<br>`<property>`<br>&nbsp;&nbsp;&nbsp;&nbsp;`<name>xxxx</name>`<br>&nbsp;&nbsp;&nbsp;&nbsp;`<value>xxxx</value>`<br>`</property>`
`<deploymentType>` | false | Specifies the deployment approach you want to use. Only `ftp` is supported right now.
`<resources>` | false | Specifies the artifacts to be deployed to your Web App; see the [Deploy via FTP](#deploy-via-ftp) section for more details.
`<failsOnError>` | false | Specifies whether to throw an exception when there are fatal errors during execution; the default value is **true**.<br>This setting helps prevent deployment failures from failing your entire Maven build.
`<allowTelemetry>` | false | Specifies whether to allow this plugin to send telemetry data; the default value is **true**.

<a name="authentication-with-azure"></a>
### Authentication with Azure

The following methods are supported for authenticating with Azure.

**Note**: Using your Maven `settings.xml` file is the recommended method for authentication because it provides the most-reliable and flexible approach.

#### Authentication Method #1: Use the Maven settings.xml file

1. Open your existing [Maven settings.xml file](https://maven.apache.org/settings.html) in a text editor, or create a new settings.xml file if one does not already exist.

2. Follow the instructions in [Create the service principal](https://docs.microsoft.com/cli/azure/create-an-azure-service-principal-azure-cli#create-the-service-principal) to create a service principal which will be used to authenticate with Azure.

3. Use the credentials from the previous step to add a new server configuration in `Servers` section of your `settings.xml` file using the following syntax:

   ```xml
   <server>
      <id>azure-auth</id>
      <configuration>
          <client>xxxx</client>
          <tenant>xxxx</tenant>
          <key>xxxx</key>
          <environment>AZURE</environment>
      </configuration>
   </server>
   ```
   Where the values for the configuration properties are listed in the following table:
   
   Property | Required | Description
   ---|---|---
   client | true | Specifies the Client ID of your service principal.
   tenant | true | Specifies the Tenant ID of your service principal.
   key | false | Specifies the password if your service principal uses password authentication.
   certificate | false | Specifies the absolute path of your certificate if your service principal uses certificate authentication.<br>**Note**: Only PKCS12 certificates are supported.
   certificatePassword | false | Specifies the password for your certificate, if there is any.
   environment | false | Specifies the target Azure cloud environment; the default value is **AZURE**.<br>The possible values are: <br>- `AZURE`<br>- `AZURE_CHINA`<br>- `AZURE_GERMANY`<br>- `AZURE_US_GOVERNMENT`
   
4. Add the following configuration settings to your `pom.xml` file:

   ```xml
   <plugin>
       <groupId>com.microsoft.azure</groupId>
       <artifactId>webapp-maven-plugin</artifactId>
       <configuration>
           <authentication>
              <serverId>azure-auth</serverId>
           </authentication>
           ...
       </configuration>
   </plugin>
   ```

#### Authentication Method #2: Use an authentication file

1. Follow instructions in [Creating a Service Principal in Azure](https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md#creating-a-service-principal-in-azure)
to create an authentication file.

2. Configure the plugin to use this file as below.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <authentication>
            <file>/absolute/path/to/auth/file</file>
         </authentication>
         <subscriptionId>your-subscription-guid</subscriptionId>
         ...
      </configuration>
   </plugin>
   ```

   **Notes**:

   * A recommended practice is to put the full path to your authentication file in your `settings.xml` file; see [Example: Injecting POM Properties via Settings.xml](https://maven.apache.org/examples/injecting-properties-via-settings.html) for details.

   * The `subscriptionId` element is an optional setting that you can use to specify which target subscription to use when there are multiple subscriptions in your authentication file. If you do not specify this setting, your default subscription in your authentication file will be used.

#### Authentication Method #3: Use the Azure CLI 2.0

1. Install the Azure CLI 2.0 by following the instructions in the [Install Azure CLI 2.0](https://docs.microsoft.com/cli/azure/install-azure-cli) article.

2. Run the following commands to log into your Azure subscription:

   ```shell
   az login
   az account set --subscription <subscription Id>
   ```
   
   You are all set. No extra configuration are required.

### Web App (Windows based)

For Windows-based Web App, only Java runtime stack is supported in our plugin. You can use `<javaVersion>` and `<javaWebContainer>` to configure your Web App.

<a name="java-runtime"></a>
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
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <javaVersion>1.8</javaVersion>
         ...
      </configuration>
   </plugin>
   ```

<a name="web-container"></a>
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
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <javaWebContainer>tomcat 8.5</javaWebContainer>
         ...
      </configuration>
   </plugin>
   ```

<a name="deploy-via-ftp"></a>
#### Deploy via FTP
You can deploy your **WAR** file and other artifacts to Web App via FTP. The following example shows all configuration elements.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <deploymentType>ftp</deploymentType>
         <resources>
            <resource>
                <directory>${project.basedir}/target</directory>
                <targetPath>/</targetPath>
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
   `targetPath` | Specifies the target path where the resources will be deployed to.<br>This is a relative path to the `/site/wwwroot/webapps` folder in your Web App server.
   `includes` | A list of patterns to include, e.g. `**/*.xml`.
   `excludes` | A list of patterns to exclude, e.g. `**/*.xml`.

### Web App on Linux

<a name="container-setting"></a>
#### Container Setting

In the `<containerSettings>` element of your `pom.xml` file, you can specify which docker container image to deploy to your Web App. Typically, this image should be from a private container registry which is built from your app, but you can also use images from a docker hub.

You can specify the following properties within the `<containerSettings>` element:

Property | Required | Description
---|---|---
`<imageName>` | true | Specifies the Docker image name. Valid image name formats are listed as below.<br>- Docker Hub image: `[hub-user/]repo-name[:tag]`; `tag` is optional, default value is **latest**.<br>- Private registry image: `hostname/repo-name[:tag]`; `tag` is optional, default value is **latest**.
`<serverId>` | false | Specifies the credentials for private docker hub images or private container registry images. (Note: `serverId` should be from your Maven `setting.xml` file.)
`<registryUrl>` | false | Specifies the URL of private container registry images.

The following examples illustrate the configuration settings for different image sources.

#### Deploy an Azure Web App with a public Docker Hub image

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <resourceGroup>yourResourceGroup</resourceGroup>
         <appName>yourWebApp</appName>
         <containerSettings>
            <imageName>nginx</imageName>
         </containerSettings>
         <appSettings>
            <property>
               <name>PORT</name>
               <value>80</value>
            </property>
         </appSettings>
      </configuration>
   </plugin>
   ```

#### Deploy an Azure Web App with a private Docker Hub image.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <resourceGroup>yourResourceGroup</resourceGroup>
         <appName>yourWebApp</appName>
         <containerSettings>
            <imageName>microsoft/nginx</imageName>
            <serverId>yourServerId</serverId>
         </containerSettings>
         <appSettings>
            <property>
               <name>PORT</name>
               <value>80</value>
            </property>
         </appSettings>
      </configuration>
   </plugin>
   ```

#### Deploy an Azure Web App with image in private registry.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>webapp-maven-plugin</artifactId>
      <configuration>
         <resourceGroup>yourResourceGroup</resourceGroup>
         <appName>yourWebApp</appName>
         <containerSettings>
            <imageName>microsoft.azurecr.io/nginx</imageName>
            <serverId>yourServerId</serverId>
            <registryUrl>https://microsoft.azurecr.io</registryUrl>
         </containerSettings>
         <appSettings>
            <property>
               <name>PORT</name>
               <value>80</value>
            </property>
         </appSettings>
      </configuration>
   </plugin>
   ```
