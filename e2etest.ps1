# Define the functions
Function RemoveFileIfExist($fileName) {
    if (Test-Path $fileName) {
        Remove-Item -Force $fileName
    }
}

Function RemoveFolderIfExist($folderName) {
    if (Test-Path $folderName) {
        Remove-Item -Recurse -Force $folderName
    }
}

Function DownloadFileFromUrl($url, $destination) {
    $wc = New-Object System.Net.WebClient
    $wc.DownloadFile($url, $destination)
}

Function UpdateMavenPluginVersion($pomLocation, $version) {
    $pomFile = gi $pomLocation
    $pom = [xml](gc $pomFile)
    $pom.project.properties.'azure.functions.maven.plugin.version' = $version
    $pom.Save($pomFile.Fullname)
}

# Scripts
$base = pwd
$functionCLIPath = "$base\Azure.Functions.Cli"
$functionCLIZipPath = "$base\Azure.Functions.Cli.zip"

# Download Functions Core Tools
RemoveFileIfExist $functionCLIZipPath
RemoveFolderIfExist $functionCLIPath
DownloadFileFromUrl $Env:FUNCTIONCLI_URL $functionCLIZipPath
Expand-Archive $functionCLIZipPath -DestinationPath $functionCLIPath
$Env:Path = $Env:Path + ";$functionCLIPath"

# Clone and install function maven plguin and archetype
$functionPom = [xml](gc ".\azure-functions-maven-plugin\pom.xml")
$functionVersion = $functionPom.project.version
mvn clean install

# Generate function project through archetype
$testProjectBaseFolder = ".\testprojects"
$testProjectPomLocation = ".\e2etestproject\pom.xml" 
RemoveFolderIfExist $testProjectBaseFolder
mkdir $testProjectBaseFolder
cd $testProjectBaseFolder
mvn archetype:generate -DarchetypeGroupId="com.microsoft.azure" -DarchetypeArtifactId="azure-functions-archetype" -DgroupId="com.microsoft" -DartifactId="e2etestproject" -Dversion="1.0-SNAPSHOT" -Dpackage="com.microsoft" -DappRegion="westus" -DresourceGroup="e2etest-java-functions-group" -DappName="e2etest-java-functions" -B
# Update e2e project pom to use the latest maven plugin
UpdateMavenPluginVersion $testProjectPomLocation $functionVersion
mvn -f $testProjectPomLocation clean package
cd ..

# Run function host
$Env:FUNCTIONS_WORKER_RUNTIME = "java"
$Env:AZURE_FUNCTIONS_ENVIRONMENT = "development"
$Env:AzureWebJobsScriptRoot = "$base\$testProjectBaseFolder\e2etestproject\target\azure-functions\e2etest-java-functions"
$proc = start-process -filepath "$functionCLIPath\func.exe" -WorkingDirectory "$Env:AzureWebJobsScriptRoot" -ArgumentList "host start" -RedirectStandardOutput "output.txt" -RedirectStandardError "error.txt" -PassThru
Start-Sleep -s 30
return $proc
