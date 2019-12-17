package com.microsoft.azure.maven.function;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import com.microsoft.azure.common.project.IProject;
import com.microsoft.azure.common.project.JavaProject;

public class MavenProjectConvert {
	public static IProject getProject(MavenProject project) {
		final JavaProject proj = new JavaProject();
		proj.setProjectName(project.getName());
		proj.setBaseDirectory(project.getBasedir().toPath());
		String jarArtifactName = project.getBuild().getFinalName() + "." + project.getPackaging();
		proj.setJarArtifact(Paths.get(project.getBuild().getDirectory(), jarArtifactName));
		proj.setClassesOutputDirectory(Paths.get(project.getBuild().getOutputDirectory()));
		proj.setDependencies(collectDependencyPaths(project.getArtifacts()));
		proj.setBuildDirectory(Paths.get(project.getBuild().getDirectory()));
		return proj;
	}
	private static List<Path> collectDependencyPaths(Set<Artifact> dependencies) {
		List<Path> results =  new ArrayList<>();
		for (Artifact artifact : dependencies) {
			results.add(artifact.getFile().toPath());
		}
		return results;
	}
}
