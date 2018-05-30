# Common Configuration

All Maven Plugins for Azure support below configuration properties.

Property | Required | Description
---|---|---
`<authentication>`| false | Specifies which authentication method to use with Azure.<br>There are three supported methods, which are described in the [Authentication with Azure](#authentication-with-azure) section.
`<subscriptionId>` | false | Specifies the target subscription.<br>Use this setting when you have multiple subscriptions in your authentication file.
`<failsOnError>` | false | Specifies whether to throw an exception when there are fatal errors during execution; the default value is **true**.<br>This setting helps prevent deployment failures from failing your entire Maven build.
`<allowTelemetry>` | false | Specifies whether to allow this plugin to send telemetry data; default value is **true**.
`<skip>` | false | Specifies whether to skip execution. Default value is **false**.

### Authentication with Azure

The following methods are supported for authenticating with Azure.

**Note**: Using your Maven `settings.xml` file is the recommended method for authentication because it provides the most-reliable and flexible approach.

#### Authentication Method #1: Use the Maven settings.xml file

1. Open your existing [Maven settings.xml file](https://maven.apache.org/settings.html) in a text editor, or create a new settings.xml file if one does not already exist.

2. Follow the instructions in [Create the service principal](https://docs.microsoft.com/cli/azure/create-an-azure-service-principal-azure-cli#create-the-service-principal) to create a service principal which will be used to authenticate with Azure.

3. Use the credentials from the previous step to add a new server configuration in `Servers` section of your `settings.xml` file using the following syntax:

   ```xml
   <server>
      <id>azure-auth</id>
      <configuration>
          <client>xxxx</client>
          <tenant>xxxx</tenant>
          <key>xxxx</key>
          <environment>AZURE</environment>
      </configuration>
   </server>
   ```
   Where the values for the configuration properties are listed in the following table:
   
   Property | Required | Description
   ---|---|---
   client | true | Specifies the Client ID of your service principal.
   tenant | true | Specifies the Tenant ID of your service principal.
   key | false | Specifies the password if your service principal uses password authentication.
   certificate | false | Specifies the absolute path of your certificate if your service principal uses certificate authentication.<br>**Note**: Only PKCS12 certificates are supported.
   certificatePassword | false | Specifies the password for your certificate, if there is any.
   environment | false | Specifies the target Azure cloud environment; the default value is **AZURE**.<br>The possible values are: <br>- `AZURE`<br>- `AZURE_CHINA`<br>- `AZURE_GERMANY`<br>- `AZURE_US_GOVERNMENT`
   
4. Add the following configuration settings to your `pom.xml` file:

   ```xml
   <plugin>
       <groupId>com.microsoft.azure</groupId>
       <artifactId>azure-webapp-maven-plugin</artifactId>
       <configuration>
           <authentication>
              <serverId>azure-auth</serverId>
           </authentication>
           ...
       </configuration>
   </plugin>
   ```

#### Authentication Method #2: Use an authentication file

1. Follow instructions in [Auth file formats](https://github.com/Azure/azure-libraries-for-java/blob/master/AUTH.md#auth-file-formats)
to create an authentication file.

2. Configure the plugin to use this file as below.

   ```xml
   <plugin>
      <groupId>com.microsoft.azure</groupId>
      <artifactId>azure-webapp-maven-plugin</artifactId>
      <configuration>
         <authentication>
            <file>/absolute/path/to/auth/file</file>
         </authentication>
         <subscriptionId>your-subscription-guid</subscriptionId>
         ...
      </configuration>
   </plugin>
   ```

   **Notes**:

   * A recommended practice is to put the full path to your authentication file in your `settings.xml` file; see [Example: Injecting POM Properties via Settings.xml](https://maven.apache.org/examples/injecting-properties-via-settings.html) for details.

   * The `subscriptionId` element is an optional setting that you can use to specify which target subscription to use when there are multiple subscriptions in your authentication file. If you do not specify this setting, your default subscription in your authentication file will be used.

#### Authentication Method #3: Use the Azure CLI 2.0

1. Install the Azure CLI 2.0 by following the instructions in the [Install Azure CLI 2.0](https://docs.microsoft.com/cli/azure/install-azure-cli) article.

2. Run the following commands to log into your Azure subscription:

   ```shell
   az login
   az account set --subscription <subscription Id>
   ```
   
   You are all set. No extra configuration are required.
