# Maven Plugins for Azure Services

This repository contains all Maven plugins for Microsoft Azure services. 

* [Plugins](#plugins)
* [Authentication](#Authentication)
* [Common Configurations](#Common-Configurations)
* [CI/CD in Azure DevOps](#CI-CD-in-Azure-DevOps)
* [Feedback and Questions](#Feedback-and-Questions)
* [Contributing](#Contributing)
* [Telemetry](#Telemetry)

For more information on authentication, common configurations, CI CD, and general plugin documentation, [see the Wiki](https://github.com/microsoft/azure-maven-plugins/wiki).

## Plugins

Maven Plugin | Maven Central Version | Build Status
---|---|---
[Maven Plugin for Azure Web Apps](./azure-webapp-maven-plugin/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22) | [![AppVeyor Webapp Plugin](https://ci.appveyor.com/api/projects/status/0vr4svfgl9u3rcaw/branch/develop?svg=true)](https://ci.appveyor.com/project/xscript/azure-maven-plugins-xt3xm)
[Maven Plugin for Azure Functions](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Functions) | [![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-functions-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-functions-maven-plugin%22) | [![AppVeyor Function Plugin](https://ci.appveyor.com/api/projects/status/5jti4qwh0j4ekh72/branch/develop?svg=true)](https://ci.appveyor.com/project/xscript/azure-maven-plugins-vvy0i)
[Maven Plugin for Azure Spring Cloud](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Spring-Cloud) | [![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-spring-cloud-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-spring-cloud-maven-plugin%22) | 


## Authentication

All the Azure Maven plugins share the same authentication logic. There are 4 authentication methods by priority order:

1. [Service Principles in plugin configuration](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#service-principles-in-plugin-configuration)
1. [Service Principles in settings.xml](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#service-principles-in-settings.xml) (Recommended for production use)
1. [Maven Plugin for Azure Account](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#maven-plugin-for-azure-account) (Default if no other method are used)
1. [Azure CLI](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#azure-cli)

For example, if you have not only Service Principles configured in your plugin configuration, but also Azure CLI installed and logged in, the Azure Maven plugins will use the Service Principles in your plugin configuration.

If no credential found, Azure Maven plugins will automatically log you in with the third method like OAuth or DeviceLogin provided by Maven Plugin for Azure Account.

### AuthType (since Web App 1.9.0)
You can specify which authentication method to use with <authType> in Maven configuration, the default value is auto, and here are all the valid values:

* service_principal
    * Will use credential specified in plugin configuration or Maven settings.xml, this is also the first priority authentication method in auto
* azure_auth_maven_plugin
    * Will use credential provided by Azure Auth Maven Plugin, it will first consume existing secret files, and will guide you auth with Oath or Device Login if you hadn't authenticated with Auth Maven Plugin before.
* azure_cli
    * Will use credential provided by Azure CLI, this could also be used in Azure Cloud Shell.
* auto
    * Will try all the auth methods in the following sequence: service_principal, azure_auth_maven_plugin(existing secret files), azure_cli, azure_auth_maven_plugin

> Maven plugin will only try the specific authentication method (except auto) if <AuthType> is set in configuration.

See the [Authentication](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication) section in our wiki for more information.

## Common Configurations
The three Maven Plugins for Azure Web App/Functions/Spring Cloud support below configuration properties.

| Property | Required | Description | Version |
| --- | --- | --- | --- | 
| \<subscriptionId> | false	| Specifies the target subscription.<br>Use this setting when you have multiple subscriptions in your authentication file. | WebApp: 0.1.0<br>Function: 0.1.0<br>Spring: 1.0.0 |
| \<allowTelemetry> | false | Specifies whether to allow this plugin to send telemetry data; default value is true. | 	WebApp: 0.1.0 <br> Function: 0.1.0 <br> Spring: 1.0.0 |
| \<auth> | false | Specifies auth configuration. For more info, please refer to [here.](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#authentication) | WebApp:0.1.0 <br> Function:0.1.0 | 
 | \<authType> | false | Specifies which authentication method to use, default value is auto. For more infos, please refer to [here.](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#authtype) | WebApp:1.9.0 | 
 | \<skip> | false | Specifies whether to skip execution. Default value is false. | WebApp: 0.1.4 <br> Function: 0.1.0 | 

## Feedback and Questions
To report bugs or request new features, file issues on [Issues](https://github.com/microsoft/azure-maven-plugins/issues). Or, ask questions on [Stack Overflow with tag azure-java-tools](https://stackoverflow.com/questions/tagged/azure-java-tools).

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once in all repositories using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Telemetry
This project collects usage data and sends it to Microsoft to help improve our products and services.
Read Microsoft's [privacy statement](https://privacy.microsoft.com/en-us/privacystatement) to learn more.
If you would like to opt out of sending telemetry data to Microsoft, you can set `allowTelemetry` to false in the plugin configuration.
Please read our [documents](https://aka.ms/azure-maven-config) to find more details.