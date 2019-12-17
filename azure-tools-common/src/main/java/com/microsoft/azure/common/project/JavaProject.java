/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.project;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;


public class JavaProject implements IProject {

	private String projectName;

	private Path baseDirectory;

	private Path classesOutputDirectory;

	private Path buildDirectory;

	private Path jarArtifact;

	private List<Path> dependencies;

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Path getBaseDirectory() {
		return baseDirectory;
	}

	public void setBaseDirectory(Path baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public Path getClassesOutputDirectory() {
		return classesOutputDirectory;
	}

	public void setClassesOutputDirectory(Path classesOutputDirectory) {
		this.classesOutputDirectory = classesOutputDirectory;
	}

	public Path getBuildDirectory() {
		return buildDirectory;
	}

	public void setBuildDirectory(Path buildDirectory) {
		this.buildDirectory = buildDirectory;
	}

	public Path getJarArtifact() {
		return jarArtifact;
	}

	public void setJarArtifact(Path jarArtifact) {
		this.jarArtifact = jarArtifact;
	}

	public List<Path> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Path> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public Collection<Path> getProjectDepencencies() {
		return dependencies;
	}

	@Override
	public boolean isWarProject() {
		return StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(jarArtifact.getFileName().toString()), "war");
	}

	@Override
	public boolean isJarProject() {
		return StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(jarArtifact.getFileName().toString()), "jar");
	}

}
