## About web.config file

> Note: This document will talk about web.config file when the user is using JAR deployment for a Windows Web App.

When the user is using JAR deployment for a Windows Web App, the plugin will generate a a `web.config` file,
this file will be saved at `%HOME%\site\wwwroot\` of your Web App.

Below is the default template of `web.config` which is used in the plugin:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <system.webServer>
        <handlers>
            <add name="httpPlatformHandler" path="*" verb="*" modules="httpPlatformHandler" resourceType="Unspecified"/>
        </handlers>
        <httpPlatform processPath="%JAVA_HOME%\bin\java.exe" arguments="-Djava.net.preferIPv4Stack=true -Dserver.port=%HTTP_PLATFORM_PORT% -jar &quot;%HOME%\site\wwwroot\app.jar&quot;">
        </httpPlatform>
    </system.webServer>
</configuration>
```