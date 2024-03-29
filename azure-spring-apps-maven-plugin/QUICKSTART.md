# Azure Spring Apps Maven Plugin Quick Start

Azure Spring Apps Maven Plugin is intended to help you easily create and update Azure Spring Apps.
 With this tooling, you could run the deploy jobs automatically with pre defined configuration.

> NOTE:
>
> More features will be coming after the initial Private Preview announcement.

### Prerequisite

- Azure Subscription, which has been whitelisted to access Azure Managed Service for Spring Apps
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
    mvn com.microsoft.azure:azure-spring-apps-maven-plugin:1.10.0:config
    ```
    1. Select module `gateway`,`auth-service` and `account-service`

        ![](./img/SelectChildModules.png)

    1. Select your subscription and Spring Apps

    1. Expose public access to gateway

        ![](./img/ExposePublicAccess.png)
    
    1. Confirm the configuration

1. Deploy the above apps with the following command

    ``` 
    mvn azure-spring-apps:deploy
    ```
    
1. You may access Piggy Metrics with the url printed in above command
