# Maven Plugins for Azure Services
[![Travis CI](https://travis-ci.org/Microsoft/azure-maven-plugins.svg?branch=master)](https://travis-ci.org/Microsoft/azure-maven-plugins/) 
[![AppVeyor](https://ci.appveyor.com/api/projects/status/qfpxt9gct33dfmns/branch/master?svg=true)](https://ci.appveyor.com/project/xscript/azure-maven-plugins)
[![codecov](https://codecov.io/gh/microsoft/azure-maven-plugins/branch/master/graph/badge.svg)](https://codecov.io/gh/microsoft/azure-maven-plugins)
[![MIT License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/Microsoft/azure-maven-plugins/blob/master/LICENSE)

### Plugins
This repository contains all Maven plugins for Microsoft Azure services. A complete list of all plugins is shown as below.

Maven Plugin | Maven Central Version
---|---
[Maven Plugin for Azure Web Apps](./azure-webapp-maven-plugin/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-webapp-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-webapp-maven-plugin%22)
[Maven Plugin for Azure Functions](./azure-functions-maven-plugin/README.md) | [![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-functions-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-functions-maven-plugin%22)

### Reporting Issues and Feedback
If you encounter any bugs with the maven plugins, please file an issue in the [Issues](https://github.com/microsoft/azure-maven-plugins/issues) section of our GitHub repo.

### Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once in all repositories using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

### Telemetry
This project collects usage data and sends it to Microsoft to help improve our products and services.
Read Microsoft's [privacy statement](https://privacy.microsoft.com/en-us/privacystatement) to learn more.
If you would like to opt out of sending telemetry data to Microsoft, you can set `allowTelemetry` to false in the plugin configuration.
Please read our [documents](https://aka.ms/azure-maven-config) to find more details.