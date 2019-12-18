package com.microsoft.azure.maven.configuration;

import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;

public class MavenRuntimeConfiguration extends RuntimeConfiguration {
	private String serverId;

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
}
