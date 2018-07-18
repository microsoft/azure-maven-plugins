### Steps
1. Open `pom.xml`, replace the value of `resource-group` and `azure-functions-name` with the real value you want.
2. Run `mvn clean package` under `HTTP` folder to build the project.
3. Run `mvn azure-functions:run` to run the project locally.
4. Use whatever tools you want to send an Http request to the function. for example: 
- Open a browser, copy the url from log to browser and add a request string `name=world`, press enter. The url should be like: 
```
http://localhost:7071/api/HttpTriggerJava?name=world
```
