# Maven Plugin for Azure Functions

[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.azure/azure-functions-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.microsoft.azure%22%20AND%20a%3A%22azure-functions-maven-plugin%22)

The Maven Plugin for Azure Functions provides seamless integration into Maven projects.

## Documentation

Visit our [Wiki](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Functions) for detailed documentation.

### Service Bus SDK binding metadata

Packaging recognizes `ServiceBusReceivedMessage`, arrays, and `List<ServiceBusReceivedMessage>` parameters
on Service Bus triggers through the SDK parameter analyzer. It marks the existing trigger with
`supportsDeferredBinding` and does not generate an additional input binding for
`ServiceBusMessageActions` parameters associated with the trigger through `@BindingName`.

With a Java library version that provides `autoCompleteMessages`, setting it to `false` emits a JSON
boolean on the trigger. The default `true`, including an explicit `true`, is omitted so the host setting
is inherited. Runtime support also requires compatible Java worker and Service Bus extension versions.

## Feedback and Questions
To report bugs or request new features, file issues on [Issues](https://github.com/microsoft/azure-maven-plugins/issues). Or, ask questions on [Stack Overflow with tag azure-java-tools](https://stackoverflow.com/questions/tagged/azure-java-tools).

## Data and Telemetry
This project collects usage data and sends it to Microsoft to help improve our products and services.
Read Microsoft's [privacy statement](https://privacy.microsoft.com/en-us/privacystatement) to learn more.
If you would like to opt out of sending telemetry data to Microsoft, you can set `allowTelemetry` to false in the plugin configuration.
Please read our [documents](https://aka.ms/azure-maven-config) to find more details.