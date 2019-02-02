# Sample usages of Maven Plugin for Azure Web Apps

#### Table of Content
* V2 configuration
   * Web App on Linux
      * [Tomcat with JRE 8](#web-app-on-linux-tomcat-v2)
      * [JRE 8](#web-app-on-linux-jre8-v2)
   * Web App on Windows
      * [Deploy War File to Tomcat](#windows-tomcat-war-deployment-v2)
   * Web App for Containers
      * [Public Docker Hub](#web-app-for-containers-public-docker-v2)
* V1 configuration (Deprecated)
   * Web App on Linux
      * [Tomcat with JRE 8](#web-app-on-linux-tomcat)
      * [JRE 8](#web-app-on-linux-jre8)
   * Web App on Windows
      * [Deploy War File to Tomcat](#windows-tomcat-war-deployment)
      * [Deploy Executable Far File](#windows-jar-deployment)
   * Web App for Containers
      * [Public Docker Hub](#web-app-for-containers-public-docker)
      * [Private Docker Hub](#web-app-for-containers-private-docker)
* [Deploy to Existing App Service Plan](#existing-app-service-plan)
* [Deploy to Web App Deployment Slot](#web-application-to-deployment-slot)

<a name="web-app-on-linux-tomcat-v2"></a>
## Web App (on Linux) with Java 8, Tomcat
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8 and Tomcat 8.5
- Deploy a **WAR** file to context path: `/${project.build.finalName}` in your Web App server
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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  <!-- Java Runtime Stack for Web App on Windows-->
                  <runtime>
                    <os>Linux</os>
                      <!-- for now only jre8 is supported for <javaVersion> of linux web app-->
                      <javaVersion>jre8</javaVersion>
                      <webContainer>tomcat 8.5</webContainer>
                    </runtime>
                  <!-- Deployment settings -->
                  <deployment>
                    <resources>
                      <resource>
                        <directory>${project.basedir}/target</directory>
                        <targetPath>${project.build.finalName}</targetPath>
                        <includes>
                          <include>*.war</include>
                        </includes>
                      </resource>
                    </resources>
                  </deployment>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-on-linux-jre8-v2"></a>
## Web App (on Linux) with Java 8 and JAR deployment
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8
- Deploy an executable jar file to `/site/wwwroot/` directory in your Web App server

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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
                  <!-- Java Runtime Stack for Web App on Windows-->
                  <runtime>
                    <os>Linux</os>
                    <javaVersion>jre8</javaVersion>
                  </runtime>
                  <!-- Deployment settings -->
                  <deployment>
                    <resources>
                      <resource>
                        <directory>${project.basedir}/target</directory>
                        <includes>
                          <include>*.jar</include>
                        </includes>
                      </resource>
                    </resources>
                  </deployment>
                  
                  <!-- This is to make sure the jar file can be released at the server side -->
                  <stopAppDuringDeployment>true</stopAppDuringDeployment>

               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```   


<a name="windows-tomcat-war-deployment-v2"></a>
## Web App (on Windows) with Java 8, Tomcat
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
- Use Java 8 and Tomcat 8.5
- Deploy the **WAR** file to context path: `/${project.build.finalName}` in your Web App server
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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>

                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>

                  <!-- Java Runtime Stack for Web App on Windows-->
                  <runtime>
                    <os>Windows</os>
                    <javaVersion>1.8</javaVersion>
                    <webContainer>tomcat 8.5</webContainer>
                  </runtime>
                  <!-- Deployment settings -->
                  <deployment>
                    <resources>
                      <resource>
                        <directory>${project.basedir}/target</directory>
                        <targetPath>${project.build.finalName}</targetPath>
                        <includes>
                          <include>*.war</include>
                        </includes>
                      </resource>
                    </resources>
                  </deployment>
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```

<a name="web-app-for-containers-public-docker-v2"></a>
## Web App for Containers with public DockerHub container image
The following configuration is applicable for below scenario:
- Reference `${azure.auth.filePath}` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Use public DockerHub image `springio/gs-spring-boot-docker:latest` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference ${azure.auth.filePath} from Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <file>${azure.auth.filePath}</file>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
                  <!-- Runtime Stack specified by Docker container image -->
                  <runtime>
                    <os>Docker</os>
                    <image>springio/gs-spring-boot-docker:latest</image>
                  </runtime>
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
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8 and Tomcat 8.5
- Use WAR to deploy **WAR** file to context path: `/${project.build.finalName}` in your Web App server
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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
                  <!-- Java Runtime Stack for Web App on Linux-->
                  <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>

                  <!-- If <warFile> is not specified, ${project.build.directory}/${project.build.finalName}.war will be used by default -->
                  <warFile>custom/absolute/path/deploy.war</warFile>

                  <!-- If <path> is not specified, the war file will be deployed to ROOT -->
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

<a name="web-app-on-linux-jre8"></a>
## Web App (on Linux) with Java 8 and JAR deployment
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8
- Use JAR to deploy an executable jar file to `/site/wwwroot/` directory in your Web App server

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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
                  <!-- Java Runtime Stack for Web App on Linux -->
                  <linuxRuntime>jre8</linuxRuntime>
                  
                  <!-- This is to make sure the jar file can be released at the server side -->
                  <stopAppDuringDeployment>true</stopAppDuringDeployment>
                  
                  <!-- If <jarFile> is not specified, ${project.build.directory}/${project.build.finalName}.jar will be used by default  -->
                  <jarFile>custom/absolute/path/deploy.jar</jarFile>
                  
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   </project>
   ```   

<a name="windows-tomcat-war-deployment"></a>
## Web App (on Windows) with Java 8, Tomcat and WAR deployment
The following configuration is applicable for below scenario:
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
- Use Java 8 and Tomcat 8.5
- Use WAR to deploy **WAR** file to context path: `/${project.build.finalName}` in your Web App server
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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>

                  <!-- Java Runtime Stack for Web App on Windows-->
                  <javaVersion>1.8</javaVersion>
                  <javaWebContainer>tomcat 8.5</javaWebContainer>

                  <!-- If <warFile> is not specified, ${project.build.directory}/${project.build.finalName}.war will be used by default -->
                  <warFile>custom/absolute/path/deploy.war</warFile>

                  <!-- If <path> is not specified, the war file will be deployed to ROOT -->
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
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
- Use Java 8
- Use JAR to deploy **JAR** file to `/site/wwwroot/` directory in your Web App server
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
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>

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



<a name="web-app-for-containers-public-docker"></a>
## Web App for Containers with public DockerHub container image
The following configuration is applicable for below scenario:
- Reference `${azure.auth.filePath}` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Use public DockerHub image `springio/gs-spring-boot-docker:latest` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference ${azure.auth.filePath} from Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <file>${azure.auth.filePath}</file>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
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
- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App for Containers
- Use private DockerHub image `your-docker-account/your-private-image:latest` as runtime stack
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.5.0</version>
               <configuration>
                  <!-- Reference <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used when creating new App Service Plan -->
                  <region>westeurope</region>
                  <pricingTier>P1V2</pricingTier>
                  
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

<a name="existing-app-service-plan"></a>
## Web App deployment to an existing App Service Plan
The following configuration is applicable for below scenario:
- Web App on Linux
- Use existing App Service Plan
- Use Java 8 and Tomcat 8.5
- Use WAR to deploy **WAR** file to ROOT: `/` in Tomcat

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
               <version>1.5.0</version>
               <configuration>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>

                  <!-- Deploy Web App to the existing App Service Plan -->
                  <appServicePlanResourceGroup>plan-resource-group</appServicePlanResourceGroup>
                  <appServicePlanName>plan-name</appServicePlanName>
                  
                  <!-- Java Runtime Stack for Web App on Linux-->
                  <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
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

- Reference `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Use Java 8 and Tomcat 8.5
- Use **WAR** deployment to deploy war file to context path `/${project.build.finalName}` in your Web App server
- Create a deployment slot and copy configuration from parent Web App then do the deploy

```xml
<project>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>com.microsoft.azure</groupId>
                <artifactId>azure-webapp-maven-plugin</artifactId>
                <version>1.5.0</version>
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
