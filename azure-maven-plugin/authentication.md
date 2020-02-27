# Authentication

To access Azure resources with different azure feature plugins, there are several methods to authenticate:

## Maven goal: *azure:login*
Use `azure:login` maven goal, if there are no credentials created by azure cli and no \<auth> configurations at each azure feature plugin’s configuration, this goal will be executed automatically.

## Maven settings 
Use service principals in maven `settings.xml`, you can use *client* and *key*(alternatively you can use *certificate* and *certificatePassword*), you can use maven to protect the *key* and *certificatePassword*, in the following sample, the *key* is encrypted by maven. This approach is the recommended approach.

```xml
<server>
   <id>azure-auth</id>
   <configuration>
       <client>df4d03fa-135b-4b7b-932d-2f2ba6449792</client>
       <tenant>72f988bf-86f1-41af-91ab-2d7cd011db47</tenant>
       <key>{FK5V3AXt9j...dq4g==}</key>
       <environment>AZURE</environment>
   </configuration>
</server>
```

> Note: The maven `settings.xml` file might be in a path like the following examples:
>   * `/etc/maven/settings.xml`
>   * `%ProgramFiles%\apache-maven\3.5.0\conf\settings.xml`
>   * `$HOME/.m2/settings.xml`

in *pom.xml*, the server id is referenced in *serverId*(please be aware that the \<auth> configuration is not supposed to be specified in azure maven plugin, but feature plugins like azure spring plugin).
```xml
<configuration>
     <auth>
         <serverId>azure-auth</serverId>
      </auth>
</configuration>
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

## Maven plugin configuration

Use service principals in \<configuration> section of azure plugins like *azure-spring-plugin* in `pom.xml`, for example:
```xml
<configuration>
    <auth>            
        <client>df4d03fa-135b-4b7b-932d-2f2ba6449792</client>
        <tenant>72f988bf-86f1-41af-91ab-2d7cd011db47</tenant>
        <key>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
        <environment>AZURE</environment>
    </auth>
</configuration>
```
> Note:  <br>If you specify both `<serverId>` and other configurations like `<client>` and `<tenant>`,  only `<serverId>` will be used, you may be reported by an error if you specified a wrong serverId.
>        <br>In `pom.xml`, you can not use maven encrypted values.

## Use Azure Cli(local & cloud shell)
   Install the Azure CLI 2.0 by following the instructions in the [Install Azure CLI 2.0](https://docs.microsoft.com/cli/azure/install-azure-cli) article.
   - Run the following commands to log into your Azure subscription：
```
    $ az login
    $ az account set --subscription <put your subscription guid here>
```
