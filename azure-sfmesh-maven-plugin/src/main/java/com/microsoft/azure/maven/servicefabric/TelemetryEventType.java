package com.microsoft.azure.maven.servicefabric;

public enum TelemetryEventType {
    INIT("Java.MavenCLI.Init"),
    ADDSERVICE("Java.MavenCLI.AddService"),
    ADDVOLUME("Java.MavenCLI.AddVolume"),
    ADDGATEWAY("Java.MavenCLI.AddGateway"),
    ADDNETWORK("Java.MavenCLI.AddNetwork"),
    ADDSECRET("Java.MavenCLI.AddSecret"),
    ADDSECRETVALUE("Java.MavenCLI.AddSecretValue"),
    DEPLOYLOCAL("Java.MavenCLI.DeployLocal"),
    DEPLOYMESH("Java.MavenCLI.DeployMesh"),
    DEPLOYSFRP("Java.MavenCLI.DeploySFRP"),
    REMOVEAPP("Java.MavenCLI.RemoveApp"),
    REMOVENETWORK("Java.MavenCLI.RemoveNetwork"),
    REMOVEGATEWAY("Java.MavenCLI.RemoveGateway"),
    REMOVEVOLUME("Java.MavenCLI.RemoveVolume"),
    REMOVESECRET("Java.MavenCLI.RemoveSecret"),
    REMOVESECRETVALUE("Java.MavenCLI.RemoveSecretValue"),;

    private final String value;

    private TelemetryEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
