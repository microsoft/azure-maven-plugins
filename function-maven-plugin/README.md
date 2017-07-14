# Azure Functions Maven Plugin

## Prerequisites

Tool | Required Version
---|---
JDK | 1.7 and above
Maven | 3.0 and above

## Goals

**TODO**

## Usage

To use the Azure Functions plugin in your Maven Java app, add the following settings for the plugin to your `pom.xml` file:

   ```xml
   <project>
      ...
      <build>
         <plugins>
            <plugin>
               <groupId>com.microsoft.azure</groupId>
               <artifactId>function-maven-plugin</artifactId>
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
