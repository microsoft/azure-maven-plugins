/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.AzureLoginTimeoutException;
import com.nimbusds.jose.util.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class LocalAuthServer {
    private static String loginSuccessHTMLTemplate;
    private static String loginErrorHTMLTemplate;
    private final Semaphore semaphore = new Semaphore(0);
    private final Server jettyServer;

    private String code;
    private String error;
    private String errorDescription;

    public LocalAuthServer() {
        if (loginErrorHTMLTemplate == null) {
            try {
                initHtmlTemplate();
            } catch (IOException e) {
                throw new RuntimeException("Cannot read html pages for local auth server.");
            }
        }
        jettyServer = new Server();
        final ServerConnector connector = new ServerConnector(jettyServer);
        connector.setHost("localhost");
        jettyServer.setConnectors(new Connector[]{ connector });
        jettyServer.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                initHtmlTemplate();
                try {
                    ((Request) request).setHandled(true);
                    code = request.getParameter(Constants.CODE);
                    // handle the error response described at
                    // https://docs.microsoft.com/en-us/azure/active-directory/develop/v1-protocols-oauth-code#error-response
                    errorDescription = request.getParameter(Constants.ERROR_DESCRIPTION);
                    error = request.getParameter(Constants.ERROR);

                    final boolean isSuccess = StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(Constants.CONTENT_TYPE_TEXT_HTML);
                    try (final PrintWriter writer = response.getWriter()) {
                        writer.write(isSuccess ? loginSuccessHTMLTemplate : String.format(loginErrorHTMLTemplate, error, errorDescription));
                        writer.flush();
                    }
                    response.flushBuffer();
                } finally {
                    semaphore.release();
                }
            }
        });
    }

    public URI getURI() {
        return jettyServer.getURI();
    }

    public void start() throws IOException {
        if (jettyServer.isStarted()) {
            return;
        }
        try {
            jettyServer.start();
        } catch (Exception e) { // server.start will throw Exception
            if (e instanceof RuntimeException) {
                // avoid unnecessary exception convert
                throw (RuntimeException) e;
            }

            throw new IOException(e);
        }

    }

    public void stop() throws IOException {
        semaphore.release();
        try {
            jettyServer.stop();
        } catch (Exception e) { // server.stop will throw Exception
            if (e instanceof RuntimeException) {
                // avoid unnecessary exception convert
                throw (RuntimeException) e;
            }

            throw new IOException(e);
        }

    }

    public String waitForCode() throws InterruptedException, AzureLoginFailureException {
        boolean timeout = false;
        if (!semaphore.tryAcquire(Constants.OAUTH_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            timeout = true;
            try {
                stop();
            } catch (IOException e) {
                // ignore
            }
        }

        if (StringUtils.isEmpty(error) && StringUtils.isNotEmpty(code)) {
            return code;
        }

        if (StringUtils.isNotEmpty(error)) {
            if (StringUtils.isNotEmpty(errorDescription)) {
                throw new AzureLoginFailureException(error + "\nReason: " + errorDescription);
            }
            throw new AzureLoginFailureException(error);
        }

        if (timeout) {
            throw new AzureLoginTimeoutException(
                    String.format("Cannot proceed with login after waiting for %d minutes.", Constants.OAUTH_TIMEOUT_MINUTES));
        }
        throw new AzureLoginFailureException("There is no error and no code.");

    }

    static void initHtmlTemplate() throws IOException {
        // don't use String.replace, which will do the regular expression replacement
        loginSuccessHTMLTemplate = StringUtils.replace(loadResource("success.html"), "${refresh_url}", Constants.LOGIN_LANDING_PAGE);
        loginErrorHTMLTemplate = StringUtils.replace(loadResource("failure.html"), "${refresh_url}", Constants.LOGIN_LANDING_PAGE);
    }

    static String loadResource(String resourceName) throws IOException {
        return IOUtils.readInputStreamToString(LocalAuthServer.class.getClassLoader().getResourceAsStream(resourceName), Constants.UTF8);
    }

}
