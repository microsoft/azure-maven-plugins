package com.microsoft.azure.common.project;

import java.nio.file.Path;
import java.util.Collection;

public interface IProject {
	Path getBaseDirectory();

	Path getJarArtifact();

	Path getBuildDirectory();

	Path getClassesOutputDirectory();

	Collection<Path> getProjectDepencencies();

	boolean isWarProject();

	boolean isJarProject();

}
