# Azure Functions Maven Plugin
[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-functions-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-functions-maven-plugin%22)

## Prerequisites

Tool | Required Version
---|---
JDK | 1.8 and above
Maven | 3.0 and above
Azure Functions Local Emulator | Latest version

## Goals

#### `azure-functions:package`
- Scan the output directory (default is `${project.basedir}/target/classes`) and generating `function.json` for each function (method annotated with `FunctionName`) in the staging directory.
- Copy JAR files from the build directory (default is `${project.basedir}/target/`) to the staging directory.

>NOTE:
>Default staging directory is `${project.basedir}/target/azure-functions/${function-app-name}/`

#### `azure-functions:run`
- Invoke Azure Functions Local Emulator to run all functions. Default working directory is the staging directory.
- Use property `-Dazure.function=myFunction` to run a single function named `myFunction`

#### `azure-functions:deploy` 
- Deploy the staging directory to the specified Function App.
- If the specified Function App does not exist already, it will be created.
 

## Usage

To use the Azure Functions plugin in your Maven Java app, add the following snippet to your `pom.xml` file:

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>azure-functions-maven-plugin</artifactId>
               <version>0.1.0</version>
               <configuration>
                  ...
               </configuration>
            </plugin>
            ...
         </plugins>
      </build>
   <project>
   ```

## Configurations
*TODO*
