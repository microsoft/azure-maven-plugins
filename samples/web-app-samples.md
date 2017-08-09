# Sample usages of Maven Plugin for Azure Web Apps

#### Table of Content
- [Web App (on Windows) with Java 8, Tomcat and FTP deployment](#web-app-on-windows)

- [Web App on Linux with public DockerHub container image](#web-app-on-linux-public-docker)

- [Web App on Linux with private DockerHub container image](#web-app-on-linux-private-docker)

- [Web App on Linux with docker container image in private container registry](#web-app-on-linux-private-registry)


<a name="web-app-on-windows"></a>
## Web App (on Windows) with Java 8, Tomcat and FTP deployment
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Windows
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
               <version>0.1.2</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used to create new Web App if the specified Web App doesn't exist -->
                  <region>westus</region>
                  <pricingTier>S1</pricingTier>
                  
                  <!-- Java Runtime Stack-->
                  <javaVersion>1.8</javaVersion>
                  <javaWebContainer>tomcat 8.5</javaWebContainer>
                  
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
   <project>
   ```

<a name="web-app-on-linux-public-docker"></a>
## Web App on Linux with public DockerHub container image
The following configuration is applicable for below scenario:
- Referencing `${azure.auth.filePath}` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
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
               <version>0.1.2</version>
               <configuration>
                  <!-- Referencing ${azure.auth.filePath} from Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <file>${azure.auth.filePath}</file>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used to create new Web App if the specified Web App doesn't exist -->
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
   <project>
   ```

<a name="web-app-on-linux-private-docker"></a>
## Web App on Linux with private DockerHub container image
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
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
               <version>0.1.2</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used to create new Web App if the specified Web App doesn't exist -->
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
   <project>
   ```

<a name="web-app-on-linux-private-registry"></a>
## Web App on Linux with docker container image in private container registry
The following configuration is applicable for below scenario:
- Referencing `<serverId>` in Maven's `settings.xml` to authenticate with Azure
- Web App on Linux
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
               <version>0.1.2</version>
               <configuration>
                  <!-- Referencing <serverId> in Maven's settings.xml to authenticate with Azure -->
                  <authentication>
                    <serverId>azure-auth</serverId>
                  </authentication>
                  
                  <!-- Web App information -->
                  <resourceGroup>your-resource-group</resourceGroup>
                  <appName>your-app-name</appName>
                  <!-- <region> and <pricingTier> are optional. They will be used to create new Web App if the specified Web App doesn't exist -->
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
   <project>
   ```
