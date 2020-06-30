# Change Log
All notable changes to the "Maven Plugin for Azure Function" will be documented in this file.
- [Change Log](#change-log)
  - [1.6.1](#161)
  - [1.6.0](#160)
  - [1.5.0](#150)
  - [1.4.1](#141)
  - [1.4.0](#140)
  - [1.3.5](#135)
  - [1.3.4](#134)
  - [1.3.3](#133)
  - [1.3.2](#132)
  - [1.3.1](#131)
  - [1.3.0](#130)
  - [1.2.2](#122)
  - [1.2.1](#121)
  - [1.2.0](#120)

## 1.6.1
- Fix default deployment method for new linux functions with existing app service plan.[#1098](https://github.com/microsoft/azure-maven-plugins/issues/1098)

## 1.6.0
- Support Java 11 Azure Functions (Preview). [#1052](https://github.com/microsoft/azure-maven-plugins/issues/1052)
- Support specify Azure environment for auth method 'azure_auth_maven_plugin'. [PR#1095](https://github.com/microsoft/azure-maven-plugins/pull/1095)

## 1.5.0
- Support creating application insights while creating or updating function apps. [PR#1074](https://github.com/microsoft/azure-maven-plugins/pull/1074)
- Fix: Failed to package function projects with runtime classpath. [#1051](https://github.com/microsoft/azure-maven-plugins/issues/1051)

## 1.4.1
- Support specify authentication method in configuration with parameter `<authType>`.[PR#975](https://github.com/microsoft/azure-maven-plugins/pull/975)
- Support creating new app service plan with specified name.[PR#1011](https://github.com/microsoft/azure-maven-plugins/pull/1011)
- Validate null subscription id and provide friendly error messages when authentication fails.[#931](https://github.com/microsoft/azure-maven-plugins/issues/931)
- Fix: Functions will do useless service plan update during deployment when the settings are not changed[#1008](https://github.com/microsoft/azure-maven-plugins/issues/1008)

## 1.4.0
- Support functions with Linux runtime.[PR#906](https://github.com/microsoft/azure-maven-plugins/pull/906)
- Support functions in docker runtime.[PR#917](https://github.com/microsoft/azure-maven-plugins/pull/917)
- Support new deployment methods: RUN_FROM_ZIP,RUN_FROM_BLOB.[PR#896](https://github.com/microsoft/azure-maven-plugins/pull/896),[PR#903](https://github.com/microsoft/azure-maven-plugins/pull/903)
- Add default value for `FUNCTIONS_EXTENSION_VERSION`.[PR#898](https://github.com/microsoft/azure-maven-plugins/pull/898)

## 1.3.5
- Support OAuth and Device Login support to auth with Azure.[PR#843](https://github.com/microsoft/azure-maven-plugins/pull/843)
- Update to `azure-function-java-library` 1.3.1. [#882](https://github.com/microsoft/azure-maven-plugins/issues/822)
- Always write `authLevel` of HTTPTrigger to `function.json`, for HttpTrigger-Java connector issues.[PR#892](https://github.com/microsoft/azure-maven-plugins/pull/892)

## 1.3.4
- Skip `func extensions install` when using extension bundles [#609](https://github.com/microsoft/azure-maven-plugins/issues/609)

## 1.3.3
- Set `FUNCTIONS_WORKER_RUNTIME` to `java` by default.[#400](https://github.com/microsoft/azure-maven-plugins/issues/400)
- Exit when no annotated methods are found in project folder.[#426](https://github.com/microsoft/azure-maven-plugins/issues/426)
- Fix: Plugin can't get correct subscription in azure cloud shell.[#628](https://github.com/microsoft/azure-maven-plugins/issues/628)
- Fix: Plugin can't get client id when user login azure cli with service principal.[#125](https://github.com/microsoft/azure-maven-plugins/issues/125)

## 1.3.2
- Fix plugin will break down in Java 11.[PR#610](https://github.com/Microsoft/azure-maven-plugins/pull/610)

## 1.3.1
- Reset AI instrumentation Key

## 1.3.0
- Add support for custom binding.
- Set java version to 1.8 during deployment.

## 1.2.2
- Updated to Java Function Lib 1.2.2.
- Fix `azure-functions:list` may not response in Linux.
- Always write cardinality to function.json for EventHubTrigger.

## 1.2.1
- Add help message,default value and validation in `function:add`. [#526](https://github.com/Microsoft/azure-maven-plugins/pull/526)
- Refactor binding to remove the strong dependency on java lib. [#456](https://github.com/Microsoft/azure-maven-plugins/issues/456)

## 1.2.0
- Show error if building extensions.csproj fails. [#417](https://github.com/Microsoft/azure-maven-plugins/issues/417)
- Update zipDeploy to use Run From Package. [#404](https://github.com/Microsoft/azure-maven-plugins/issues/404)
- Update Function Java Library to 1.2.0.
- Add support for new attributes of EventHubTrigger and CosmosDB annotations. [#412](https://github.com/Microsoft/azure-maven-plugins/issues/412), [#420](https://github.com/Microsoft/azure-maven-plugins/issues/420)
- Remove NotificationHub and MobileTable for it is not supported.
