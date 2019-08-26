# Azure Spring Cloud Maven Plugin Quick Start

Azure Spring Cloud Maven Plugin is intended to help you easily create and update Azure Spring Cloud Services.
 With this tooling, you could run the deploy jobs automatically with pre defined configuration.

> NOTE:
>
> More features will be coming after the initial Private Preview announcement.

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

1. Add the following configuration to correspond pom.xml.

    - *account-service*
    
    ```pom
    <plugin>
    	<groupId>com.microsoft.azure</groupId>
    	<artifactId>azure-spring-maven-plugin</artifactId>
    	<version>0.0.1-SNAPSHOT</version>
    	<configuration>
    		<subscriptionId>${SUBSCRIPTION_ID}</subscriptionId>
    		<clusterName>${CLUSTER}</clusterName>
    		<appName>${project.artifactId}</appName>
    		<isPublic>false</isPublic>
    		<deployment>
    			<cpu>1</cpu>
    			<memoryInGB>2</memoryInGB>
    			<instanceCount>1</instanceCount>
    			<jvmOptions>-Xmx1G</jvmOptions>
    			<resources>
    				<resource>
    					<directory>${project.basedir}/target</directory>
    					<includes>
    						<include>account-service.jar</include>
    					</includes>
    				</resource>
    			</resources>
    		</deployment>
    	</configuration>
    </plugin>
    ```
    
    - *auth-service*
    
    ```pom
    <plugin>
    	<groupId>com.microsoft.azure</groupId>
    	<artifactId>azure-spring-maven-plugin</artifactId>
    	<version>0.0.1-SNAPSHOT</version>
    	<configuration>
    		<subscriptionId>${SUBSCRIPTION_ID}</subscriptionId>
    		<clusterName>${CLUSTER}</clusterName>
    		<appName>${project.artifactId}</appName>
    		<isPublic>false</isPublic>
    		<deployment>
    			<cpu>1</cpu>
    			<memoryInGB>2</memoryInGB>
    			<instanceCount>1</instanceCount>
    			<jvmOptions>-Xmx1G</jvmOptions>
    			<resources>
    				<resource>
    					<directory>${project.basedir}/target</directory>
    					<includes>
    						<include>auth-service.jar</include>
    					</includes>
    				</resource>
    			</resources>
    		</deployment>
    	</configuration>
    </plugin>
    ```
    
    - *gateway*
    
    ```pom
    <plugin>
    	<groupId>com.microsoft.azure</groupId>
    	<artifactId>azure-spring-maven-plugin</artifactId>
    	<version>0.0.1-SNAPSHOT</version>
    	<configuration>
    		<subscriptionId>${SUBSCRIPTION_ID}</subscriptionId>
    		<clusterName>${CLUSTER}</clusterName>
    		<appName>${project.artifactId}</appName>
    		<isPublic>true</isPublic>
    		<deployment>
    			<cpu>1</cpu>
    			<memoryInGB>2</memoryInGB>
    			<instanceCount>1</instanceCount>
    			<jvmOptions>-Xmx1G</jvmOptions>
    			<resources>
    				<resource>
    					<directory>${project.basedir}/target</directory>
    					<includes>
    						<include>gateway.jar</include>
    					</includes>
    				</resource>
    			</resources>
    		</deployment>
    	</configuration>
    </plugin>
    ```
    
> You may use `mvn com.microsoft.azure:azure-spring-maven-plugin:0.0.1-SNAPSHOT:config` to generate configuration, in this case, you need to speicfy resource in each pom as spring cloud maven plugin will use artifact with project's final name. 
1. Deploy the above apps with the following command

    ``` 
    mvn com.microsoft.azure:azure-spring-maven-plugin:0.0.1-SNAPSHOT:deploy
    ```
    
1. You may access Piggy Metrics with the url printed in above command

### Next Steps

Learn more about Azure Managed Service for Spring Cloud by reading below links.
- [HOW-TO guide of using Azure Managed Service for Spring Cloud](./docs/how-to.md)
- [Developer Guide](./docs/dev-guide.md)
- [FAQ](./docs/faq.md)