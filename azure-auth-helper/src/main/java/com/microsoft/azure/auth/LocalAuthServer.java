/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.nimbusds.jose.util.IOUtils;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.Map;

@SuppressWarnings("restriction")
public class LocalAuthServer {
    private static final String ERROR = "error";
    private static final String CODE = "code";
    // TODO: need to update this URL after we post the login quick start at
    // docs.microsoft.com
    private static final String LOGIN_DOC_URL = "https://docs.microsoft.com/en-us/java/api/overview/azure/maven/azure-webapp-maven-plugin/readme/";
    private static transient boolean inited = false;
    private static Object initLock = new Object();
    private static String loginSuccessBodyTemplate;
    private static String loginErrorBodyTemplate;

    private HttpServer server;
    private String code;
    private String error;
    private String errorDescription;
    private boolean finish;


    public LocalAuthServer() throws IOException {
        initHtmlTemplate();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", httpExchange -> {
            try {
                final Map<String, String> attributeMap = QueryStringUtil.queryToMap(httpExchange.getRequestURI().getQuery());
                LocalAuthServer.this.code = attributeMap.get(CODE);
                LocalAuthServer.this.error = attributeMap.get(ERROR);
                LocalAuthServer.this.errorDescription = attributeMap.containsKey("error_description") ? attributeMap.get("error_description")
                        : attributeMap.get("error_response");

                final boolean isSuccess = StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code);


                final OutputStreamWriter osw = new OutputStreamWriter(httpExchange.getResponseBody());
                if (isSuccess) {
                    httpExchange.sendResponseHeaders(200, loginSuccessBodyTemplate.length());
                    osw.write(loginSuccessBodyTemplate);
                    osw.flush();
                } else {
                    final String response = String.format(loginErrorBodyTemplate, error, errorDescription);
                    httpExchange.sendResponseHeaders(200, response.length());
                    osw.write(response);
                    osw.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                server.stop(0);
                finish = true;
            }
        });
        server.setExecutor(null); // creates a default executor
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getUrl() {
        return "http://localhost:" + getPort();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public String getResult() throws InterruptedException, AzureLoginFailureException {
        while (!finish) {
            Thread.sleep(50);
        }
        if (StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code)) {
            return code;
        }
        if (StringUtils.isNotEmpty(error)) {
            if (StringUtils.isNotEmpty(errorDescription)) {
                throw new AzureLoginFailureException(error + "\nReason: " + errorDescription);
            }
            throw new AzureLoginFailureException(error);
        } else {
            throw new AzureLoginFailureException("There is no error and no code.");
        }
    }

    static void initHtmlTemplate() throws IOException {
        if (inited) {
            return;
        }

        synchronized (initLock) {
            if (!inited) {
                // don't use String.replace, which will do regex replacement
                loginSuccessBodyTemplate = StringUtils.replace(loadResource("success.html"), "${refresh_url}", LOGIN_DOC_URL);
                loginErrorBodyTemplate = StringUtils.replace(loadResource("failure.html"), "${refresh_url}", LOGIN_DOC_URL);
                inited = true;
            }
        }
    }

    static String loadResource(String resourceName) throws IOException {
        return IOUtils.readInputStreamToString(LocalAuthServer.class.getClassLoader().getResourceAsStream(resourceName), Constants.UTF8);
    }

}
