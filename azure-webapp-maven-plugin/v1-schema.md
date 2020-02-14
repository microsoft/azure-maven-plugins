## Webapp configuration V1 Schema(Deprecated)

### Config

Property | Required | Description | Version
---|---|---|---
`<region>` | false | Specifies the region where your Web App will be hosted; the default value is **westeurope**. All valid regions at [Supported Regions](#region) section. | 0.1.0+
`<resourceGroup>` | true | Azure Resource Group for your Web App. | 0.1.0+
`<appName>` | true | The name of your Web App. | 0.1.0+
`<pricingTier>` | false | The pricing tier for your Web App. The default value is **P1V2**.| 0.1.0+
`<deploymentSlot>` | false | The deployment slot to deploy your application. | 1.3.0+
`<appServicePlanResourceGroup>` | false | The resource group of the existing App Service Plan. If not specified, the value defined in `<resourceGroup>` will be used by default. | 1.0.0+
`<appServicePlanName>` | false | The name of the existing App Service Plan. | 1.0.0+
`<appSettings>` | false | Specifies the application settings for your Web App. | 0.1.0+
`<stopAppDuringDeployment>` | false | To stop the target Web App or not during deployment. This will prevent deployment failure caused by IIS locking files. | 0.1.4+
  

### Runtime settings

  Details about the supported values could be found [here](README.md). Tomcat 8.5 will be used as default value for `<javaWebContainer>`.
  
- **Web App on Linux**
  ```xml
  <configuration>
  ...
    <linuxRuntime>tomcat 8.5-jre8</linuxRuntime>
  </configuration>
  ```
  The supported values are *tomcat 8.5-jre8*, *tomcat 9.0-jre8*, *jre8*, *tomcat 8.5-java11*, *tomcat 9.0-java11*, *java11*.

- **Web App on Windows**
  ```xml
  <configuration>
  ...
    <javaVersion>1.8</javaVersion>
    <javaWebContainer>tomcat 8.5</javaWebContainer>
  </configuration>
  ```

- **Web App for Containers**    
  ```xml
  <configuration>
  ...
    <containerSettings>
    <!-- only the imageName is required --> 
      <imageName>[hub-user/]repo-name[:tag]</imageName>
      <serverId></serverId>
      <registryUrl></registryUrl>
    </containerSettings>
  </configuration>
  ```

    `tag` is optional for the `imageName`. The default value is latest. Public docker hubs and private container registries are both supported.


### Deployment settings

  There are multiple deployment types are supported:
    
- ZIP Deploy
    
  ZIP deploy is intended for fast and easy deployments.
  ```xml
  <configuration>
  ...
    <deploymentType>zip</deploymentType>
    <resources>
      <resource>
        <directory>${project.basedir}/target</directory>
        <includes>
          <include>*.jar</include>
        </includes>
        <excludes>
          <exclude>*.xml</exclude>
        </excludes>
      </resource>
    </resources>
  </configuration>
  ```
- WAR Deploy
    
  Both `<warFile>` and `<path>` are optional. By default it will find the war file according to the `<finalName>` in the  project build directory, and deploy to ROOT.
  ```xml
  <configuration>
  ...
    <deploymentType>war</deploymentType>
    <warFile></warFile>
    <path></path>
  </configuration>
  ```
- JAR Deploy
    
  `<jarFile>` is not required. If not specified, it will deploy the `${project.build.directory}/${project.build.finalName}.jar` to `%HOME%\site\wwwroot\` of your Web App. 
  > Please note that for Windows Web App, we will generate a `web.config` file, you can find more details [here](../docs/web-config.md).
  ```xml
  <configuration>
    ...
    <deploymentType>jar</deploymentType>
    <jarFile></jarFile>
  </configuration>
  ```
    
- AUTO Deploy

  This is the default deployment type used by the plugin. It will inspect `<packaging>` field in the pom file to decide how to deploy the artifact. If the `<packaging>` is set to `war`, the plugin will use war deployment. If the `<packaging>` is set to `jar`, the plugin will use jar deployment. Otherwise, the plugin will skip the deployment, which is the same as `NONE` deployment.

  If you want the plugin to inspect the `<packaging>` field. Just don't set `<deploymentType>`. The plugin will use `AUTO` deployment as default.


- NONE

  NONE means do not need deploy, so just skip deployment.

- FTP
 
  You can deploy your artifacts/resources to Web App via FTP.
  ```xml
  <configuration>
    ...
    <deploymentType>ftp</deploymentType>
    <resources>
      <resource>
        <directory>${project.basedir}/target</directory>
        <includes>
          <include>*.jar</include>
        </includes>
        <excludes>
          <exclude>*.xml</exclude>
        </excludes>
      </resource>
    </resources>
  </configuration>
  ```

### Samples
You could find v1 samples [here](../docs/web-app-samples-v1.md).
