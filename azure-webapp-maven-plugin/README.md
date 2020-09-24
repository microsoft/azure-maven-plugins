# Maven Plugin for Azure App Service

## Overview
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22)

The Maven Plugin for Azure App Service helps Java developers to deploy Maven projects to [Azure App Service](https://docs.microsoft.com/en-us/azure/app-service/).


## Documentation

Please go through [Quickstart](https://docs.microsoft.com/en-us/azure/app-service/quickstart-java) to create your first Java app on Azure App Service, there are more [Java documents](https://github.com/microsoft/azure-maven-plugins/wiki/Java-App-Service-Documents-List) for Azure App Serice.

:book: You can also visit our [Wiki](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Web-App) for detailed documentation about Maven Webapp plugin.


## Quickstart
You can prepare your application ready for Azure App Service easily with one command(y):

```shell
mvn com.microsoft.azure:azure-webapp-maven-plugin:1.11.0:config
```

This command adds a `azure-webapp-maven-plugin` plugin and related configuration by prompting you to select an existing Azure Web App or create a new one. Then you can deploy your Java app to Azure using the following command:
```shell
mvn package azure-webapp:deploy
```


## Feedback and Questions
To report bugs or request new features, file issues on [Issues](https://github.com/microsoft/azure-maven-plugins/issues). Or, ask questions on [Stack Overflow with tag azure-java-tools](https://stackoverflow.com/questions/tagged/azure-java-tools).

## Data and Telemetry
This project collects usage data and sends it to Microsoft to help improve our products and services.
Read Microsoft's [privacy statement](https://privacy.microsoft.com/en-us/privacystatement) to learn more.
If you would like to opt out of sending telemetry data to Microsoft, you can set `allowTelemetry` to false in the plugin configuration.
Please read our [documents](https://aka.ms/azure-maven-config) to find more details.
