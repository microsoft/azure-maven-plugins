# Change Log
All notable changes to the "Maven Plugin for Azure App Service" will be documented in this file.
- [Change Log](#change-log)
  - [1.6.0](#160)
  - [1.5.4](#154)
  - [1.5.3](#153)
  - [1.5.2](#152)
  - [1.5.1](#151)
  - [1.5.0](#150)
  - [1.4.1](#141)
  - [1.4.0](#140)
  - [1.3.0](#130)
  - [1.2.0](#120)
  - [1.1.0](#110)
  - [1.0.0](#100)

## 1.6.0
- Support Java 11 AppService.[PR#606](https://github.com/Microsoft/azure-maven-plugins/pull/606)
- Generate web.config and rename jar file for JavaSE runtime with schema V2.[PR#629](https://github.com/Microsoft/azure-maven-plugins/pull/629)
- Fix plugin will break down in Java 11.[PR#610](https://github.com/Microsoft/azure-maven-plugins/pull/610)
- Fix `webapp:config` retained content that the user discarded.[#620](https://github.com/Microsoft/azure-maven-plugins/pull/620)

## 1.5.4
- Reset AI instrumentation Key

## 1.5.3
- Update config UI to fit AppService Portal.
- Fix `webapp:config` may not response for project with schema V1.

## 1.5.2
- Add `webapp:config` to init and update plugin configuration.
- Fix NPE issue when deploy to an existing project without runtime configuration.

## 1.5.1
- Change the default pricing tier of Web App to P1V2.
- The region is only required when creating a new App Service Plan in schema V2. [#499](https://github.com/Microsoft/azure-maven-plugins/issues/499)

## 1.5.0
- Add new schema version V2 with refactoring configurations.
- Add new runtime WildFly 14 for Web App on Linux.
- Change the default region to `West Europe`.
- Fix the output message incorrect issue when deploying a Web App without updating it.

## 1.4.1
- Updated the Azure SDK version to v1.15.1.

## 1.4.0
- Support zip deploy.
- Refactor output message when `<deploymentType>` is set to NONE.
- Fix NPR when `<targetPath>` not set in `<resource>`.
- Output the web app URL when deployment is finished.

## 1.3.0
- Support deploy web application to deployment slot
- Add new deployment types: *AUTO* *JAR* *NONE*
- Refactor output messages during deployment

## 1.2.0
- Add new Linux Web App runtime - jre8 [#185](https://github.com/Microsoft/azure-maven-plugins/issues/185)
- Fix a bug that make Web App for Containers deploy fail [#156](https://github.com/Microsoft/azure-maven-plugins/issues/156)

## 1.1.0
- Fix the issue that token will be printed during deployment. [#129](https://github.com/Microsoft/azure-maven-plugins/issues/129)
- Support war deploy

## 1.0.0
- Add the support for deploying Web App to an existing App Service Plan

