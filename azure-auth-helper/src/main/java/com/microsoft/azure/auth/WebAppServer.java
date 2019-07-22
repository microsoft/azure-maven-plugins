/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.Map;

@SuppressWarnings("restriction")
public class WebAppServer {
    private static final String ERROR = "error";
    private static final String CODE = "code";
    // TODO: need to update this URL after we post the login quick start at
    // docs.microsoft.com
    private static final String LOGIN_DOC_URL = "https://docs.microsoft.com/en-us/java/api/overview/azure/maven/azure-webapp-maven-plugin/readme/";
    private static final String LOGIN_SUCCESS_BODY = "\n" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\" />\n" +
            "    <meta http-equiv=\"refresh\" content=\"10;url=" +
            LOGIN_DOC_URL +
            "/\">\n" +
            "    <title>Login successfully</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h4>You have logged into Microsoft Azure!</h4>\n" +
            "    <p>You can close this window, or we will redirect you to the " +
            "<a href=\"" +
            LOGIN_DOC_URL +
            "\">" +
            "Azure Maven Plugin documents</a> " +
            "in 10 seconds.</p>\n" +
            "</body>\n" +
            "</html>";

    private static final String LOGIN_ERROR_BODY = "\n" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\" />\n" +
            "    <meta http-equiv=\"refresh\" content=\"10;url=" +
            LOGIN_DOC_URL +
            "/\">\n" +
            "    <title>Login failed</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h4>%s</h4>\n" +
            "    <p>%s</p>\n" +
            "</body>\n" +
            "</html>";

    private HttpServer server;
    private String code;
    private String error;
    private String errorDescription;
    private boolean finish;

    public WebAppServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                final Map<String, String> attributeMap = QueryStringUtil.queryToMap(httpExchange.getRequestURI().getQuery());
                WebAppServer.this.code = attributeMap.get(CODE);
                WebAppServer.this.error = attributeMap.get(ERROR);
                WebAppServer.this.errorDescription = attributeMap.containsKey("error_description") ? attributeMap.get("error_description")
                        : attributeMap.get("error_response");

                final boolean isSuccess = StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code);
                try {

                    final OutputStreamWriter osw = new OutputStreamWriter(httpExchange.getResponseBody());
                    if (isSuccess) {
                        httpExchange.sendResponseHeaders(200, LOGIN_SUCCESS_BODY.length());
                        osw.write(LOGIN_SUCCESS_BODY);
                        osw.flush();
                    } else {
                        final String response = String.format(LOGIN_ERROR_BODY, error, errorDescription);
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

}
