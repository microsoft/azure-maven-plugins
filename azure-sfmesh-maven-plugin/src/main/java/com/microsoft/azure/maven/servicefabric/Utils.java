/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.servicefabric;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
public class Utils
{
    enum ResourceType{
        application, volume, network;
    }
    public static void createDirectory(Log logger ,String directoryPath) throws MojoFailureException{
        try {
            Files.createDirectory(Paths.get(directoryPath));
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException(String.format("Error while creating directory %s", directoryPath));
        }
    }

    public static boolean checkIfExists(String path){
            if(Files.exists(Paths.get(path))){
                return true;
            }
            else{
                return false;
            }
    }

    public static void deleteFileOrDirectory(String path, Log logger) throws MojoFailureException {
        try {
            FileUtils.deleteDirectory(path);
        } catch (IOException e) {
            logger.error("Directory deletion failed");
            throw new MojoFailureException(String.format("Error while deleting directory %s", path));
        }
    }
    public static String replaceString(Log logger, String content, String originalString, String replaceString, String resourceFileName){
        logger.debug(String.format("replacing %s with %s in %s", originalString, replaceString, resourceFileName));
        return content.replace(originalString, replaceString);
    }

    public static String getServicefabricResourceDirectory(Log logger, MavenProject project) throws MojoFailureException {
        return Paths.get(project.getBasedir().toString(), "servicefabric").toString();
    }

    public static String getAppResourcesDirectory(Log logger, MavenProject project) throws MojoFailureException {
        return Paths.get(getServicefabricResourceDirectory(logger, project), "appresources").toString();
    }

    public static String getPath(String directoryPath, String fileOrDirName){
        return Paths.get(directoryPath, fileOrDirName).toString();
    }

    public static String executeCommand(Log logger,String command) throws MojoFailureException{
        try {
            logger.info(String.format("Executing command %s", command));
            Process p;
            if(Utils.isWindows()){
                p = Runtime.getRuntime().exec("cmd.exe /C" +command);
            }
            else{
                p = Runtime.getRuntime().exec(command);
            }
            p.waitFor();
            int exitCode = p.exitValue();
            String stderr = IOUtil.toString(p.getErrorStream(), "UTF-8");
            String stdout = IOUtil.toString(p.getInputStream(), "UTF-8");
            logger.debug(String.format("STDOUT: %s", stdout));
            if(stderr != null && stderr.length() > 0 ){
                if(exitCode != 0){
                    logger.error(String.format("Process exited with exit code %d", exitCode));
                    logger.error(String.format("If STDERR: %s", stderr));
                    throw new MojoFailureException(String.format("Error while running the %s command", command));
                }
                else{
                    logger.info(String.format("Else STDERR: %s", stderr));
                }
            }
            return stdout;
		} catch (IOException e){
            logger.error(e);
            throw new MojoFailureException(String.format("Error while running the %s command", command));
        } catch (InterruptedException e) {
            logger.error(e);
            throw new MojoFailureException(String.format("Interrupted while running command %s", command));
        }
    }

    public static String executeCommand(Log logger,String[] command) throws MojoFailureException{
        try {
            logger.info(String.format("Executing command %s", Arrays.toString(command)));
            Process p;
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            int exitCode = p.exitValue();
            String stderr = IOUtil.toString(p.getErrorStream(), "UTF-8");
            String stdout = IOUtil.toString(p.getInputStream(), "UTF-8");
            logger.debug(String.format("STDOUT: %s", stdout));
            if(stderr != null && stderr.length() > 0 ){
                if(exitCode != 0){
                    logger.error(String.format("Process exited with exit code %d", exitCode));
                    logger.error(String.format("STDERR: %s", stderr));
                    throw new MojoFailureException(String.format("Error while running the %s command", Arrays.toString(command)));
                }
                else{
                    logger.info(String.format("STDERR: %s", stderr));
                }
            }
            return stdout;
		} catch (IOException e){
            logger.error(e);
            throw new MojoFailureException(String.format("Error while running the %s command", Arrays.toString(command)));
        } catch (InterruptedException e) {
            logger.error(e);
            throw new MojoFailureException(String.format("Interrupted while running command %s", Arrays.toString(command)));
        }
    }
    public static void checksfctlinstallation(Log logger) throws MojoFailureException{
        if(Utils.isWindows()){
            Utils.executeCommand(logger, "sfctl --help  > NUL 2>&1");
        }
        else{
            Utils.executeCommand(logger, "sfctl --help >> /dev/null 2>&1");
        }
    }

    public static void checkazinstallation(Log logger) throws MojoFailureException{
        if(Utils.isWindows()){
            Utils.executeCommand(logger, "az mesh --help > NUL 2>&1");
        }
        else{
            Utils.executeCommand(logger, "az mesh --help >> /dev/null 2>&1");
        }
    }

    public static void connecttounsecurecluster(Log logger, String endpoint) throws MojoFailureException{
        Utils.executeCommand(logger, "sfctl cluster select --endpoint " + endpoint);
    }

    public static void connecttosecurecluster(Log logger, String endpoint, String pempath) throws MojoFailureException{
        Utils.executeCommand(logger, "sfctl cluster select --endpoint " + endpoint + "--pem " + pempath);
    }

    public static String getOS(){
     return System.getProperty("os.name").toLowerCase();   
    }
    public static boolean isWindows() {
		return (getOS().indexOf("win") >= 0);
	}
	public static boolean isLinux() {
        String OS = getOS();
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") >= 0|| OS.indexOf("mac") >= 0 );	
	}

    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, Object> stringToYaml(Log logger, String content) throws MojoFailureException {
        ObjectMapper oMapper = new ObjectMapper(new YAMLFactory());
        InputStream stream = new ByteArrayInputStream(content.getBytes(Charset.forName("UTF-8")));
        try {
            return oMapper.readValue(stream, LinkedHashMap.class);
        } catch (IOException e) {
            logger.error(e);
            throw new MojoFailureException(String.format("string to yaml conversion failed"));
        }
    }

    public static String yamlToString(LinkedHashMap<String, Object> yaml){
        StringWriter content = new StringWriter();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml dumper = new Yaml (options);
        dumper.dump(yaml, content);
        return content.toString();
    }
}