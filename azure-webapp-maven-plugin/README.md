# Maven Plugin for Azure App Service

## Overview
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22)

The Maven Plugin for Azure App Service helps Java developers to deploy Maven projects to [Azure App Service](https://docs.microsoft.com/en-us/azure/app-service/).


## Documentation

Please go through [Quickstart](https://docs.microsoft.com/en-us/azure/app-service/quickstart-java) to create your first Java app on Azure App Service, there are more [Java documents](https://github.com/microsoft/azure-maven-plugins/wiki/Java-App-Service-Documents-List) for Azure App Serice.

:book: You can also visit our [Wiki](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Web-App) for detailed documentation about Maven Webapp plugin.

## Authentication
For the easiest way, you can install [Azure Cli](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest), and sign-in using:

```shell
az login
```
Mavan plugins supports Azure Cli and some other auth methods, see [Authentication](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication) for details.


## Quickstart
You can prepare your application ready for Azure Web App easily with one command:

```shell
mvn com.microsoft.azure:azure-webapp-maven-plugin:1.11.0:config
```

This command adds a `azure-webapp-maven-plugin` plugin and related configuration by prompting you to select an existing Azure Web App or create a new one. Then you can deploy your Java app to Azure using the following command:
```shell
mvn package azure-webapp:deploy
```

## Configuration
Here is a typical configuration for Azure Web App Maven Plugin:
```xml
<plugin> 
  <groupId>com.microsoft.azure</groupId>  
  <artifactId>azure-webapp-maven-plugin</artifactId>  
  <version>1.12.0</version>  
  <configuration>
    <schemaVersion>V2</schemaVersion>
    <subscriptionId>111111-11111-11111-1111111</subscriptionId>
    <resourceGroup>spring-boot-xxxxxxxxxx-rg</resourceGroup>
    <appName>spring-boot-xxxxxxxxxx</appName>
    <pricingTier>B2</pricingTier>
    <region>westus</region>
    <runtime>
      <os>Linux</os>      
      <webContainer>Java SE</webContainer>
      <javaVersion>Java 11</javaVersion>
    </runtime>
    <deployment>
      <resources>
        <resource>
          <directory>${project.basedir}/target</directory>
          <includes>
            <include>*.jar</include>
          </includes>
        </resource>
      </resources>
    </deployment>
  </configuration>
</plugin> 
```

Property | Required | Description 
---|---|---
`<schemaVersion>` | false | Specify the version of the configuration schema. The default value and recommended value is  `V2`  |
`<subscriptionId>` | false | Specifies the target subscription.<br>Use this setting when you have multiple subscriptions in your authentication file.|
`<resourceGroup>` | true | Azure Resource Group for your Web App. |
`<appName>` | true | The name of your Web App. |
`<pricingTier>`| false | The pricing tier for your Web App. The default value is **P1V2**.|
`<region>`| false | Specifies the region where your Web App will be hosted; the default value is **westeurope**. All valid regions at [Supported Regions](#region) section. |
`<os>`| false | Specifies the os, supported values are Linux, Windows and Docker. |
`<webContainer>`| false | Specifies the runtime stack, values for Linux are: *Tomcat 8.5*, *Tomcat 9.0*, *Java SE*, *JbossEAP 7.2*|
`<javaVersion>`| false | Specifies the java version, values are: *Java 8* or *Java 11*|
`<deployment>`| false | Specifies the target file to be deployed |

## Feedback and Questions
To report bugs or request new features, file issues on [Issues](https://github.com/microsoft/azure-maven-plugins/issues). Or, ask questions on [Stack Overflow with tag azure-java-tools](https://stackoverflow.com/questions/tagged/azure-java-tools).

## Data and Telemetry
This project collects usage data and sends it to Microsoft to help improve our products and services.
Read Microsoft's [privacy statement](https://privacy.microsoft.com/en-us/privacystatement) to learn more.
If you would like to opt out of sending telemetry data to Microsoft, you can set `allowTelemetry` to false in the plugin configuration.
Please read our [documents](https://aka.ms/azure-maven-config) to find more details.
