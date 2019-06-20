# Change Log
All notable changes to the "Maven Plugin for Azure Function" will be documented in this file.
- [Change Log](#change-log)
  - [1.3.3](#133)
  - [1.3.2](#132)
  - [1.3.1](#131)
  - [1.3.0](#130)
  - [1.2.2](#122)
  - [1.2.1](#121)
  - [1.2.0](#120)

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
