# Azure Spring Cloud Maven Plugin Quick Start

Azure Spring Cloud Maven Plugin helps you easily create and update Azure Spring Cloud Services.
 This plugin enables clean integration between CI/CD pipelines and the Azure Spring Cloud Service.

> NOTE:
>
> Look out for more features after the initial Private Preview announcement.

### Prerequisite

- Azure Subscription, which has been whitelisted to access Azure Managed Service for Spring Cloud
- JDK 8 installed on your local machine
- Maven 3.0 and above installed on your local machine


### Provision service instance
### Set Git URL for config server
Please refer this [document](https://github.com/Azure/azure-managed-service-for-spring-cloud-docs#provision-service-instance) to provision service instance and set config server. 

### Build and Deploy microservices applications

1. Clone git repository by running below command.
    ```
    git clone https://github.com/xscript/PiggyMetrics
    ```
  
1. Change directory and build the project by running below command.
    ```
    cd PiggyMetrics
    mvn clean package -DskipTests
    ```
1. Add the following configurations in your pom.xml or setting.xml to access plugin snapshot.
    ```
    <pluginRepositories>
      <pluginRepository>
        <id>maven.snapshots</id>
        <name>Maven Central Snapshot Repository</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        <releases>
          <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
      </pluginRepository>
    </pluginRepositories>
    ```
    
1. Generate configuration by running the below command.
    ```
    mvn com.microsoft.azure:azure-spring-cloud-maven-plugin:0.1.0-SNAPSHOT:config
    ```
    1. Select module `gateway`,`auth-service` and `account-service`

        ![](./img/SelectChildModules.png)

    1. Select your subscription and spring cloud service cluster

    1. Expose public access to gateway

        ![](./img/ExposePublicAccess.png)
    
    1. Confirm the configuration

1. Deploy the above apps with the following command

    ``` 
    mvn com.microsoft.azure:azure-spring-cloud-maven-plugin:0.1.0-SNAPSHOT:deploy
    ```
    
1. You may access Piggy Metrics with the url printed in above command
