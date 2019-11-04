# Maven Plugin for Azure Spring Cloud

## Table of Content
  - [Overview](#overview)
    - [Azure Spring Cloud](#azure-spring-cloud-service-overview)
    - [Azure Spring Cloud Maven Plugin](#azure-spring-plugin-overview)
  - [Prerequisites](#prerequisites)
    - [System Requirements](#azure-spring-plugin-requirements)
    - [Create Azure Spring Cloud](#azure-spring-service-provisioning)
  - [Authentication](#authentication)
  - [Configure Your Applications](#configuration)
  - [Deploy to Azure](#deploy)
  - [Goals](#goals)
    - [`azure-spring-cloud:config`](#azure-spring-config)
    - [`azure-spring-cloud:deploy`](#azure-spring-deploy)
  - [Feedback and Questions](#feedback-and-questions)
  - [Data and Telemetry](#data-and-telemetry)

## Overview

<a name="azure-spring-cloud-service-overview"></a>
### Azure Spring Cloud
Azure Spring Cloud makes it easy to deploy Spring Boot-based microservice applications to Azure with zero code changes. Azure Spring Cloud allows developers to focus on their code by managing the lifecycle of Spring Cloud applications. Spring Cloud provides lifecycle management using comprehensive monitoring and diagnostics, configuration management, service discovery, CI/CD integration, blue-green deployments, and more.


<a name="azure-spring-plugin-overview"></a>
### Azure Spring Cloud Maven Plugin
This Azure Spring Cloud Maven plug-in helps developer configure and deploy microservices applications to Azure Spring Cloud.

<a name="prerequisites"></a>
## Prerequisites

<a name="azure-spring-plugin-requirements"></a>
### System Requirements 
Tool | Required Version
---|---
JDK | 1.8 
Maven | 3.x.x

<a name="azure-spring-service-provisioning"></a>
### Provision a service instance on the Azure portal
1. In a web browser, open the [Azure portal](https://portal.azure.com), and sign in to your account.

1. Search for and then select **Azure Spring Cloud**. 
1. On the overview page, select **Create**, and then do the following:  

    a. In the **Service Name** box, specify the name of your service instance. The name must be from 4 to 32 characters long and can contain only lowercase letters, numbers, and hyphens. The first character of the service name must be a letter, and the last character must be either a letter or a number.  

    b. In the **Subscription** drop-down list, select the subscription you want to be billed for this resource. Ensure that this subscription has been added to our allow list for Azure Spring Cloud.  

    c. In the **Resource group** box, create a new resource group. Creating resource groups for new resources is a best practice.  

    d. In the **Location** drop-down list, select the location for your service instance. Currently supported locations include East US, West US 2, West Europe, and Southeast Asia.
    
It takes about 5 minutes for the service to be deployed. After the service is deployed, the **Overview** page for the service instance appears.

<a name="authentication"></a>
## Authentication 
To authenticate, use the [`azure:login`](../azure-maven-plugin/README.md#azure-login) goal, or refer to [this document](../azure-maven-plugin/authentication.md) for more details.

<a name="configuration"></a>
## Configure Your Applications
In order to deploy an App, you will need to configure it, eg: the name of Spring Cloud Service, the App name etc. The maven goal `azure-spring-cloud:config` can help you to do this job through a step by step wizard, following with these steps will make your project ready for the deployment. You can refer to the **[config](#azure-spring-config)** for detailed usage


<a name="deploy"></a>
## Deploy to Azure
Deploy an App to Azure Spring Cloud is easy, you can run `mvn azure-spring-cloud:deploy` command after you build the project by `mvn package`, the azure spring maven plugin will upload your artifact to Azure Spring Cloud and then start it. It will take several minutes to deploy the App service, please be patient. You can refer to the **[deploy](#azure-spring-deploy)** for detailed usage

<a name="goals"></a>
## Goals

<a name="azure-spring-config"></a>
### `azure-spring-cloud:config`
- This goal will set up the plugin configuration used in `azure-spring-cloud:deploy` through a step by step wizard. It will require you to login using any way described in [Authentication Methods](#Authentication) section. You will proceed with the following steps, after you have finished these steps, the configuration for *azure-spring-cloud-maven-plugin*  will be filled with values you selected during the wizard, then you can execute `azure-spring-cloud:deploy`. With **default** mode, you will be promoted with the following steps:

- select or confirm default subscription: **subscriptionId** (if there is only one subscription available in your account, the **subscriptionId** will be set automatically)
- select or confirm Azure Spring Cloud: **clusterName**(if there is only one cluster available in your subscription, the **clusterName** will be set automatically)
- provide appName or accept default as **artifactId**
- provide whether or not to expose the public access to this app


With **advanced** mode by executing `mvn azure-spring-cloud:config -DadvancedOptions`, you will be promoted with some additional steps:

* provide the following parameters (press ENTER to use default value):
    * instanceCount
    * cpu
    * memoryInGB
    * jvmOptions

All the settings will be validated before writing to the `pom.xml`, you should get no configuration error when you executing `deploy` goal.

 > Note:  <br> - the pom.xml will be modified by this goal if you select *yes* in last step(by default is **yes**).
 >        <br> - if you have no cluster created, this goal will fail with error messages.
 >        <br> - if you don't have *azure-spring-cloud-maven-plugin* in  **\<build>\<plugins>** section, a new *azure-spring-maven-cloud-plugin* section will be added to the existing *pom.xml* with configuration collected. 
 >        <br> - if you have already existing properties configured in **azure-spring-cloud-maven-plugin**, the *config* goal will exit without errors(this means you cannot **config** twice for the same project unless you delete the configuration manually), the **\<auth>** section in **\<configuration>** is not considered as spring configuration.
 >        <br> The properties not asked in the steps will be filled with default values.
 >        <br> It is OK to have no **\<auth>** section, in this case, this goal will seek credentials from `azure-secret.json`, *azure cli token* and cloud shell token(MSI Credential). If none of above ways fails, this goal will trigger an `azure:login` goal to get the credential. If this project has an **\<auth>** section, this goal will only get credentials from configuration from **\<auth>** section, if there are any errors during getting credentials, this goal will fail.

#### Multi-jar behavior
- If executed in parent folder, you will be promoted with the following steps:
    - select one or more projects which are supposed to be deployed as an app.
    - select or confirm the default subscription: **subscriptionId**
    - select or confirm the Azure Spring Cloud(ASC): **clusterName**(if there is only one ASC available in your subscription, the **clusterName** will be set automatically)
    - select some apps which are supposed to be **public** accessible.
    

 > Note:  <br> The *advanced* mode will not be support at parent folder.
 >        <br> All the changes will be save to each child *pom.xml* files once you select *yes* in last step(by default is **yes**). The parent *pom.xml* will be updated in **azure-spring-cloud** plugin with no **configuration**.
 >        <br> If you run this goal on a partially configured folder(some of the projects have **\<configuration>** section and some of the projects don't), the projects with  **\<configuration>** section will not be selectable and thus cannot be reconfigured.
 >        <br> The properties not asked in the steps will be filled with default values.
 >        <br> This goal will do nothing if all the projects are already configured.
 >        <br> If you have not selected any projects, this goal will exit successfully.

<a name="azure-spring-deploy"></a>
### `azure-spring-cloud:deploy`
- This goal triggers a deploy in Azure Spring Cloud. Before you execute *deploy* goal, you need to have the spring cluster created. This goal will create the app if the app doesn't exist(the default app name is the *artifactId* in *pom.xml*), upload the jar at *target* folder and update all the configuration in app and deployment. It will start the app if it is previously stopped. If the same jar is already deployed, only the configuration will be updated(it may not be implemented in version 1.0.0, but will be supported in later versions). This goal will quit until the deployment enters the final state: **Succeeded** or **Failed**. If the app is public and deployed successfully, this goal will print an endpoint of this app before exit. 

App level Property | Required | Description
---|---|---
subscriptionId | false | Specifies the target subscription, if not specified, the default subscription will be used
clusterName | true | Specifies the cluster name.
appName | false | Specifies the app name, if not specified, the artifactId will be used
public | false | Indicates whether the app exposes public endpoint
deployment| false | DeploymentSettings

Deployment level Property | Required | Type| Description
---|---|---|---
cpu | false| int | cpu cores
runtimeVersion| false | The runtime version
memoryInGB|  false| int | memory in GB
jvmOptions|  false| string | jvm options
instanceCount|  false| int | instance count
environment|  false| key-value | environment variables
enablePersistentStorage | false | boolean | whether or not to mount a persistent storage to `/persistent` folder(volume quota of 50 GB)
resources | true | array of Resource | specifies where the resources are to be deployed.

Resource level Property | Description
---|---
`directory` | Specifies where the resources are stored.
`includes` | A list of patterns to include, e.g. `**/*.jar`.
`excludes` | A list of patterns to exclude, e.g. `**/-test.jar`.

Here are a sample configuration:


```xml
<groupId>com.microsoft.azure</groupId>
<artifactId>azure-spring-cloud-maven-plugin</artifactId>
<version>1.0.0</version>
<configuration>
    <subscriptionId>00000000-0000-0000-0000-000000000000</subscriptionId>
    <clusterName>testCluster1</clusterName>
    <appName>helloworld</appName>
    <isPublic>true</isPublic>>
    <enablePersistentStorage>false<enablePersistentStorage>
    <deployment>
        <cpu>1</cpu>
        <memoryInGB>4</memoryInGB>
        <instanceCount>1</instanceCount>
        <runtimeVersion>8</runtimeVersion>
        <jvmOptions>-XX:+UseG1GC -XX:+UseStringDeduplication</jvmOptions>
        <environment>
            <foo>bar</foo>
        </environment>
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

``` 

 > Note: <br> - if the ASC does not exist, this goal will fail with error messages.
 >       <br> - if you have not yet built the jar by `package` goal, this goal will fail with error messages.
 >       <br> - if this jar in *target* folder is not executable-jar , this goal will fail with error messages.
 >       <br> - This goal requires you to login using any way described in Authentication Methods section, if you have no configuration in \<auth> section, this goal will check if you have ever logged in with “azure:login” goal,  and if not, if will then check if you have ever logged in through azure cli, if not, it will execute “azure:login” goal and then continue the deploy process.


#### Multi-jar behavior

- If executed in parent folder
    - Deploy all sub-modules one by one.
- If executed in child folder
    - Only deploy that project.

## FeedBack and Questions
If you encounter any bugs with the maven plugins, please file an issue in the [Issues](https://github.com/microsoft/azure-maven-plugins/issues) section of our GitHub repo.

## Data and Telemetry
This project collects usage data and sends it to Microsoft to help improve our products and services.
Read Microsoft's [privacy statement](https://privacy.microsoft.com/en-us/privacystatement) to learn more.
If you would like to opt out of sending telemetry data to Microsoft, you can set `allowTelemetry` to false in the plugin configuration.
Please read our [documents](https://aka.ms/azure-maven-config) to find more details.
