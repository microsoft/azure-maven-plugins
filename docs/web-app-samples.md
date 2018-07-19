# Sample usages of Maven Plugin for Azure Web Apps

#### Table of Content
* Web App on Windows
  * [Deploy War File to Tomcat](#windows-tomcat-war-deployment)
  * [Deploy Executable Far File](#windows-jar-deployment)
* Web App on Linux
  * [Tomcat with JRE 8](#web-app-on-linux-tomcat)
  * [JRE 8](#web-app-on-linux-jre8)
* Web App for Containers
  * [Public Docker Hub](#web-app-for-containers-public-docker)
  * [Private Docker Hub](#web-app-for-containers-private-docker)
  * [Private Container Registry](#web-app-for-containers-private-registry)
* [Deploy to Existing App Service Plan](#existing-app-service-plan)
* [Deploy to Web App Deployment Slot](#web-application-to-deployment-slot)

<a name="windows-tomcat-war-deployment"></a>
## Web App (on Windows) with Java 8, Tomcat and WAR deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
- Using Java 8 and Tomcat 8.5
- Using WAR to deploy **WAR** file to context path: `/${project.build.finalName}` in your Web App server
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <packaging>war</packaging>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>

                  <!-- Java Runtime Stack for Web App on Windows-->
                  <javaVersion>1.8</javaVersion>
                  <javaWebContainer>tomcat 8.5</javaWebContainer>

                  <!-- If <warFile> is not specified, ${project.build.directory}/${project.build.finalName}.war will be used by default -->
                  <warFile>custom/absolute/path/deploy.war</warFile>

                  <!-- Specify context path, optional if you want to deploy to ROOT -->
                  <path>/${project.build.finalName}</path>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                     <property>
                        <name>your-setting-key</name>
                        <value>your-setting-value</value>
                     </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```
   
<a name="windows-jar-deployment"></a>
## Web App (on Windows) with Java 8 and JAR deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
- Using Java 8
- Using JAR to deploy **JAR** file to context path: `/${project.build.finalName}` in your Web App server
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <packaging>jar</packaging>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>

                  <!-- Java Runtime Stack for Web App on Windows-->
                  <javaVersion>1.8</javaVersion>

                  <!-- If <jarFile> is not specified, ${project.build.directory}/${project.build.finalName}.jar will be used by default -->
                  <jarFile>custom/absolute/path/deploy.jar</jarFile>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                     <property>
                        <name>your-setting-key</name>
                        <value>your-setting-value</value>
                     </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-on-linux-tomcat"></a>
## Web App (on Linux) with Java 8, Tomcat and WAR deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Using Java 8 and Tomcat 8.5
- Using WAR to deploy **WAR** file to ROOT: `/` in your Web App server
  > Note: Currently the **Linux** Web App with Tomcat runtime only supports deploy to ROOT. If you specify <path> in the plugin configurations, it will not take effect.
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <packaging>war</packaging>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>
                  
                  <!-- Java Runtime Stack for Web App on Linux-->
                  <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>

                  <!-- If <warFile> is not specified, ${project.build.directory}/${project.build.finalName}.war will be used by default -->
                  <warFile>custom/absolute/path/deploy.war</warFile>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                     <property>
                        <name>your-setting-key</name>
                        <value>your-setting-value</value>
                     </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```
   
<a name="web-app-on-linux-jre8"></a>
## Web App (on Linux) with Java 8 and JAR deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Using Java 8
- Using JAR to deploy an executable jar file to `/site/wwwroot/` directory in your Web App server
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <packaging>jar</packaging>
      ...
      <build>
         <finalName>app</finalName>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>
                  
                  <!-- Java Runtime Stack for Web App on Linux -->
                  <linuxRuntime>jre8</linuxRuntime>
                  
                  <!-- This is to make sure the jar file can be released at the server side -->
                  <stopAppDuringDeployment>true</stopAppDuringDeployment>
                  
                  <!-- If <jarFile> is not specified, ${project.build.directory}/${project.build.finalName}.jar will be used by default  -->
                  <jarFile>custom/absolute/path/deploy.jar</jarFile>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                      <property>
                          <name>JAVA_OPTS</name>
                          <value>-Djava.security.egd=file:/dev/./urandom</value>
                      </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```   

<a name="web-app-for-containers-public-docker"></a>
## Web App for Containers with public DockerHub container image
The following configuration is applicable for below scenario:
- Referencing `${azure.auth.filePath}` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Using public DockerHub image `springio/gs-spring-boot-docker:latest` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing ${azure.auth.filePath} from Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <file>${azure.auth.filePath}</file>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>
                  
                  <!-- Runtime Stack specified by Docker container image -->
                  <containerSettings>
                     <imageName>springio/gs-spring-boot-docker:latest</imageName>
                  </containerSettings>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                     <property>
                        <name>PORT</name>
                        <value>8080</value>
                     </property>
                     <property>
                        <name>your-setting-key</name>
                        <value>your-setting-value</value>
                     </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-for-containers-private-docker"></a>
## Web App for Containers with private DockerHub container image
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Using private DockerHub image `your-docker-account/your-private-image:latest` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>
                  
                  <!-- Runtime Stack specified by Docker container image -->
                  <containerSettings>
                     <imageName>your-docker-account/your-private-image:latest</imageName>
                     <serverId>docker-auth</serverId>
                  </containerSettings>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                     <property>
                        <name>PORT</name>
                        <value>8080</value>
                     </property>
                     <property>
                        <name>your-setting-key</name>
                        <value>your-setting-value</value>
                     </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-for-containers-private-registry"></a>
## Web App for Containers with docker container image in private container registry
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Using image `example.azurecr.io/image-name:latest` from private container registry `https://example.azurecr.io` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>
                  
                  <!-- Runtime Stack specified by Docker container image -->
                  <containerSettings>
                     <!-- Image name should include the private registry URL -->
                     <imageName>example.azurecr.io/image-name:latest</imageName>
                     <!-- Referencing serverId from settings.xml to authenticate with your private container registry -->
                     <serverId>private-registry-auth</serverId>
                     <!-- Private registry URL should include protocol HTTP or HTTPS -->
                     <registryUrl>https://example.azurecr.io</registryUrl>
                  </containerSettings>
                  
                  <!-- Application Settings of your Web App -->
                  <appSettings>
                     <property>
                        <name>PORT</name>
                        <value>8080</value>
                     </property>
                     <property>
                        <name>your-setting-key</name>
                        <value>your-setting-value</value>
                     </property>
                  </appSettings>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="existing-app-service-plan"></a>
## Web App deployment to an existing App Service Plan
The following configuration is applicable for below scenario:
- Web App on Linux
- Using existing App Service Plan
- Using Java 8 and Tomcat 8.5
- Using FTP to deploy **WAR** file to `/site/wwwroot/webapps/` directory in your Web App server

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.3.0</version>
               <configuration>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>

                  <!-- Deploy Web App to the existing App Service Plan -->
                  <appServicePlanResourceGroup>plan-resource-group</appServicePlanResourceGroup>
                  <appServicePlanName>plan-name</appServicePlanName>
                  
                  <!-- Java Runtime Stack for Web App on Linux-->
                  <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
                  
                  <!-- FTP deployment -->
                  <deploymentType>ftp</deploymentType>
                  <!-- Resources to be deployed to your Web App -->
                  <resources>
                     <resource>
                        <!-- Where your artifacts are stored -->
                        <directory>${project.basedir}/target</directory>
                        <!-- Relative path to /site/wwwroot/ -->
                        <targetPath>webapps</targetPath>
                        <includes>
                           <include>*.war</include>
                        </includes>
                     </resource>
                  </resources>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name = "web-application-to-deployment-slot"></a>
## Deploy to Web App Deployment Slot
The following configuration is applicable for below scenario:

- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Using Java 8 and Tomcat 8.5
- Using **WAR** deployment to deploy war file to context path `/${project.build.finalName}` in your Web App server
- Create a deployment slot and copy configuration from parent Web App then do the deploy

```xml
<project>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>com.microsoft.azure</groupId>
                <artifactId>azure-webapp-maven-plugin</artifactId>
                <version>1.3.0</version>
                <configuration>
                    <authentication>
                        <serverId>azure-auth</serverId>
                    </authentication>
                    
                    <!-- Web App information -->
                    <resourceGroup>your-resource-group</resourceGroup>
                    <appName>your-webapp-name</appName>

                    <!-- Deployment Slot Setting -->
                    <deploymentSlotSetting>
                        <slotName>pure-slot-name</slotName>
                        <configurationSource>parent</configurationSource>
                    </deploymentSlotSetting>
                    
                    <!-- Java Runtime Stack for Web App on Linux-->
                    <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
                    
                    <!-- War Deploy -->
                    <deploymentType>war</deploymentType>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
