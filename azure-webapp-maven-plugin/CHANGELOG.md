# Change Log
All notable changes to the "Maven Plugin for Azure App Service" will be documented in this file.
- [Change Log](#change-log)
  - [2.0.0](#210)
  - [1.12.0](#1120)
  - [1.11.0](#1110)
  - [1.10.0](#1100)
  - [1.9.1](#191)
  - [1.9.0](#190)
  - [1.8.0](#180)
  - [1.7.0](#170)
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

## 2.0.0
- Deprecate deployment <resources>
- Support one deploy api
- Remove V1 Schema

## 1.12.0
- Support JBoss 7.2(EAP) within Linux Azure Web App
- Support new runtime configuration style

## 1.11.0
- Support select existing web app in `config` goal [#1161](https://github.com/microsoft/azure-maven-plugins/pull/1161)

## 1.10.0
- Support set Azure environment in configuration (`<configuration> -> <auth> -> <environment>`)
- Fixes : XSS issue in authentication [#1110](https://github.com/microsoft/azure-maven-plugins/issues/1110)
- Fixes : Resource Group name match uses case sensitive string comparison [#1056](https://github.com/microsoft/azure-maven-plugins/issues/1056)

## 1.9.1
- Remove support for WildFly for Web App on Linux.
- Fix: There is no artifact to deploy in staging directory.[#1032](https://github.com/microsoft/azure-maven-plugins/issues/1032)
- Fix: Plugin will do useless during deployment update when the settings are not changed.[#1008](https://github.com/microsoft/azure-maven-plugins/issues/1008)

## 1.9.0
- Support deploy external resources to Azure outside `wwwroot`.[PR#953](https://github.com/microsoft/azure-maven-plugins/pull/953)
- Support specify authentication method in configuration with parameter `<authType>`.[#PR#975](https://github.com/microsoft/azure-maven-plugins/pull/975)
- Validate null subscription id and provide friendly error messages when authentication fails.[#931](https://github.com/microsoft/azure-maven-plugins/issues/931)
- Fix: Updating pricingTier only if user specified it in configuration.[#908](https://github.com/microsoft/azure-maven-plugins/issues/908),[#927](https://github.com/microsoft/azure-maven-plugins/issues/927)

## 1.8.0
- Support OAuth and Device Login support to auth with Azure.[PR#843](https://github.com/microsoft/azure-maven-plugins/pull/843)
- Support native Windows Java SE app service.[PR#850](https://github.com/microsoft/azure-maven-plugins/pull/850)
- `config` will skip unsupported value according to project configuration.[PR#850](https://github.com/microsoft/azure-maven-plugins/pull/850)
- Fix: `config` does not support lowercase characters.[#745](https://github.com/microsoft/azure-maven-plugins/issues/745)
- Fix: Plugin will always modify `pom.xml` in project folder.[#757](https://github.com/microsoft/azure-maven-plugins/issues/757)
- Fix: Fail to rename windows java se artifact to `app.jar`.[PR#865](https://github.com/microsoft/azure-maven-plugins/pull/865)
- Fix: Plugin did not clean staging folder after deployment.[#869](https://github.com/microsoft/azure-maven-plugins/issues/869)

## 1.7.0
- Support customize java options for Windows Java SE app service.[#640](https://github.com/microsoft/azure-maven-plugins/issues/640)
- Support update app service plan for existing app service.[PR#677](https://github.com/microsoft/azure-maven-plugins/pull/677)
- Generate version tag if `<plugin>` is absent while `webapp:config`.[#660](https://github.com/microsoft/azure-maven-plugins/issues/660)
- Optimization prompt messages.[#667](https://github.com/microsoft/azure-maven-plugins/issues/667)
- Fix: Plugin can't get correct subscription in azure cloud shell.[#628](https://github.com/microsoft/azure-maven-plugins/issues/628)
- Fix: Plugin can't get client id when user login azure cli with service principal.[#125](https://github.com/microsoft/azure-maven-plugins/issues/125)

## 1.6.0
- Support Java 11 AppService.[PR#606](https://github.com/Microsoft/azure-maven-plugins/pull/606)
- Generate web.config and rename jar file for JavaSE runtime with schema V2.[PR#629](https://github.com/Microsoft/azure-maven-plugins/pull/629)
- Fix plugin will break down in Java 11.[PR#610](https://github.com/Microsoft/azure-maven-plugins/pull/610)
- Fix `webapp:config` retained content that the user discarded.[#620](https://github.com/Microsoft/azure-maven-plugins/pull/620)

## 1.5.4
- Reset AI instrumentation Key

## 1.5.3
- Update config UI to fit AppService Portal.
- Fix `webapp:config` may not respond for projects with schema V1.

## 1.5.2
- Add `webapp:config` to init and update plugin configuration.
- Fix NPE issue when deploying to an existing project without runtime configuration.

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
- Support deploy web applications to a deployment slot
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

