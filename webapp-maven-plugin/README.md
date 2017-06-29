# Azure Web Apps Maven Plugin

The Web Apps plugin is used to deploy container images to Azure Web Apps on Linux. It aims to provide seamlessly integration of Azure Web Apps into Maven.

Like Azure Web Apps on Linux, this plugin is still in preview. Only Linux-based Web Apps are supported right now. More features are on the way.

## Prerequisite
Tool | Required Version
---|---
JDK | 1.7 and above
Maven | 3.0 and above

## Goals
This plugin only has one goal, which is `webapp:deploy`. It is bounded to the `deploy` phase. 

Goal | Description
--- | ---
webapp:deploy | Deploy docker container image to an Azure Web App based on your configuration.<br>If the specified Web App does not exist, it will be created.

## Usage

In you Maven Java app, add the following plugin in your pom.xml:

    <project>
        ...
        <build>
            <plugins>
                <plugin>
                    <groupId>com.microsoft.azure</groupId>
                    <artifactId>webapp-maven-plugin</artifactId>
                    <version>0.1.0-alpha</version>
                    <configuration>
                        ...
                    </configuration>
                </plugin>
                ...
            </plugins>
        </build>
    <project>

## Configuration

This plugin supports below configuration properties.

Properties | Required | Description
---|---|---
`<authentication>`| false | Configure how to authenticate with Azure. Three approaches are supported. Read more details at [Authentication with Azure](#authentication-with-azure)
`<subscriptionId>` | false | Configure the target subscription. Use it when you have multiple subscriptions in your authentication file.
`<resourceGroup>` | true | Configure the target resource group.
`<appName>` | true | Configure the target Web App.
`<region>` | false | Configure the region of your Web App. Default value is **westus**.<br>It will be used to create a new Web App. If the Web App already exists, it will be ignored.
`<pricingTier>` | false | Configure the pricing tier of your Web App. Default value is **S1**.<br>It will be used to create a new Web App. If the Web App already exists, it will be ignored.
`<containerSetting>` | true | Configure to deploy which docker container image to your Web App.<br>Both docker hub and private container registry are supported. Read more details at [Container Setting](#container-setting)
`<appSettings>` | false | Configure application settings of your Web App. Define name-value pairs like below:<br>`<property>`<br>&nbsp;&nbsp;&nbsp;&nbsp;`<name>xxxx</name>`<br>&nbsp;&nbsp;&nbsp;&nbsp;`<value>xxxx</value>`<br>`</property>`
`<failsOnError>` | false | Configure whether to throw exception when there are fatal errors during execution. Default value is true.<br>Use it when you don't want deployment failure to fail your whole maven build.
`<allowTelemetry>` | false | Configure whether to allow this plugin to send telemetry data. Default value is true.

### Authentication with Azure

Below approaches are supported to authenticate with Azure.
Using Maven settings.xml is recommended, because it is the most reliable and flexible approach.

#### Use Maven settings.xml
1. Create or find your [Maven settings.xml](https://maven.apache.org/settings.html).
2. Follow instructions at [here](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli#create-the-service-principal)
   to create a service principal, which will be used to authenticate with Azure.
3. Use credentials from step 2 to add a new server configuration in `Servers` section as below.
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
   Complete configuration properties are listed in below table.
   
   Property | Description
   --- | ---
   client | Client Id of your service principal
   tenant | Tenant Id of your service principal
   key | Password if your service principal uses password authentication
   certificate | Absolute path of your certificate if your service principal uses certificate authentication.<br>Only PKCS12 certificate is supported.
   certificatePassword | Password to your certificate if there is any
   environment | Target Azure cloud environment. Optional, default value is AZURE.<br>Allowed values are: <br>- AZURE<br>- AZURE_CHINA<br>- AZURE_GERMANY<br>- AZURE_US_GOVERNMENT
   
4. Add below configuration in your pom.xml
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

#### Use authentication File
1. Follow instructions at [here](https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md#creating-a-service-principal-in-azure)
to create your authentication file.
2. Configure plugin to use this file as below.

   It is recommended to put your authenticate file path into your `Settings.xml` like [this example](https://maven.apache.org/examples/injecting-properties-via-settings.html).

   `subscriptionId` is optional. You can specify the target subscription when there are multiple subscriptions in your authentication file.
   If not specified, your default subscription in the authentication file will be used.

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

#### Use Azure CLI 2.0
1. Install [Azure CLI 2.0](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
2. Run below commands to log in your Azure subscription.
    ```shell
    az login
    az account set --subscription <subscription Id>
    ```

### Container Setting

In `<containerSetting>` tag, you can configure the docker container image which will be used to deploy your Web App. Typically, it should be an image from a private container registry, which is built from your app. But you still have the options to use images from docker hub.

Within the `<containerSetting>` tag, four properties can be configured.
Properties | Required | Description
`<imageName>` | true | Docker image name.
`<serverId>` | false | Configure the credentials for private docker hub images or private container registry images. `serverId` should be from Maven's setting.xml.
`registryUrl` | false | Configure the URL of private container registry images.

Below examples shows the configuration for different image sources.

#### Deploy an Azure Web App with public Docker Hub image

    <plugin>
        <groupId>com.microsoft.azure</groupId>
        <artifactId>webapp-maven-plugin</artifactId>
        <configuration>
            <resourceGroup>yourResourceGroup</resourceGroup>
            <appName>yourWebApp</appName>
            <containerSetting>
                <imageName>nginx</imageName>
            </containerSetting>
            <appSettings>
                <property>
                    <name>PORT</name>
                    <value>80</value>
                </property>
            </appSettings>
        </configuration>
    </plugin>

#### Deploy an Azure Web App with private Docker Hub image.

    <plugin>
        <groupId>com.microsoft.azure</groupId>
        <artifactId>webapp-maven-plugin</artifactId>
        <configuration>
            <resourceGroup>yourResourceGroup</resourceGroup>
            <appName>yourWebApp</appName>
            <containerSetting>
                <imageName>microsoft/nginx</imageName>
                <serverId>yourServerId</serverId>
            </containerSetting>
            <appSettings>
                <property>
                    <name>PORT</name>
                    <value>80</value>
                </property>
            </appSettings>
        </configuration>
    </plugin>

#### Deploy an Azure Web App with image in private registry.

    <plugin>
        <groupId>com.microsoft.azure</groupId>
        <artifactId>webapp-maven-plugin</artifactId>
        <configuration>
            <resourceGroup>yourResourceGroup</resourceGroup>
            <appName>yourWebApp</appName>
            <containerSetting>
                <imageName>microsoft.azurecr.io/nginx</imageName>
                <serverId>yourServerId</serverId>
                <registryUrl>https://microsoft.azurecr.io</registryUrl>
            </containerSetting>
            <appSettings>
                <property>
                    <name>PORT</name>
                    <value>80</value>
                </property>
            </appSettings>
        </configuration>
    </plugin>
