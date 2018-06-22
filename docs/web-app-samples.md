# Sample usages of Maven Plugin for Azure Web Apps

#### Table of Content
- [Web App (on Windows) with Java 8, Tomcat and WAR deployment](#web-app-on-windows)

- [Web App (on Linux) with Java 8, Tomcat and FTP deployment](#web-app-on-linux-tomcat)

- [Web App (on Linux) with Java 8 and FTP deployment](#web-app-on-linux-jre8)

- [Web App for Containers with public DockerHub container image](#web-app-for-containers-public-docker)

- [Web App for Containers with private DockerHub container image](#web-app-for-containers-private-docker)

- [Web App for Containers with docker container image in private container registry](#web-app-for-containers-private-registry)

- [Deploy Web App to an existing App Service Plan](#existing-app-service-plan)

<a name="web-app-on-windows"></a>
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
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.2.0</version>
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
                  
                  <!-- WAR deployment -->
                  <deploymentType>war</deploymentType>

                  <!-- Specify the war file location, optional if the war file location is: ${project.build.directory}/${project.build.finalName}.war -->
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
   
<a name="web-app-on-linux-tomcat"></a>
## Web App (on Linux) with Java 8, Tomcat and FTP deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Using Java 8 and Tomcat 8.5
- Using FTP to deploy **WAR** file to `/site/wwwroot/webapps/` directory in your Web App server
- Add Application Settings to your Web App

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.2.0</version>
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
## Web App (on Linux) with Java 8 and FTP deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
- Using Java 8
- Using FTP to deploy an executable jar file to `/site/wwwroot/` directory in your Web App server
> Note: Please make sure your jar file name is `app.jar`, this is the name that Web App server will search and execute.
- Add Application Settings to your Web App
> Note: Currently we need to make sure the `JAVA_OPTS` contains `-Djava.security.egd=file:/dev/./urandom`, otherwise the Web App might not successfully start up.

   ```xml
   <project>
      ...
      <build>
         <finalName>app</finalName>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-webapp-maven-plugin</artifactId>
               <version>1.2.0</version>
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
                  
                  <!-- FTP deployment -->
                  <deploymentType>ftp</deploymentType>
                  <!-- Resources to be deployed to your Web App -->
                  <resources>
                     <resource>
                        <!-- Where your artifacts are stored -->
                        <directory>${project.basedir}/target</directory>
                        <!-- Relative path to /site/wwwroot/ -->
                        <targetPath>/</targetPath>
                        <includes>
                           <include>app.jar</include>
                        </includes>
                     </resource>
                  </resources>
                  
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
               <version>1.2.0</version>
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
               <version>1.2.0</version>
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
               <version>1.2.0</version>
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
               <version>1.2.0</version>
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