/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.jaxrs;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.repository.testutils.PortUtil;

import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;

/* These tests do not test any specific Hippo functionality, each show a "small" example of how to embed and test
 * either a servlet embedded in Tomcat, a JAXRS resource served by CXF embedded in Tomcat, or the
 * RepositoryJaxrsServlet embedded in Tomcat.
 */
public class EmbeddingTest {

    private static final Logger[] silencedTomcatLoggers = new Logger[] {
            Logger.getLogger("org.apache.tomcat.util.net.NioSelectorPool"),
            Logger.getLogger("org.apache.catalina.util.SessionIdGeneratorBase")
    };

    static {
        for (Logger l : silencedTomcatLoggers) {
            l.setLevel(Level.WARNING);
        }
    }

    @Rule
    public TemporaryFolder tmpTomcatFolder = new TemporaryFolder();

    private String getTmpTomcatFolderName() {
        return tmpTomcatFolder.getRoot().getAbsolutePath();
    }

    @Test
    public void embedTomcat() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setSilent(true);
        tomcat.setBaseDir(getTmpTomcatFolderName());
        int portNumber = PortUtil.getPortNumber(getClass());
        tomcat.setPort(portNumber);

        Context context = tomcat.addContext("", getTmpTomcatFolderName());
        Tomcat.addServlet(context, "embedTomcat", new HttpServlet() {
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.setContentType("text/plain");
                Writer w = resp.getWriter();
                w.write("Hello world from plain servlet");
                w.flush();
            }
        });
        context.addServletMapping("/embedTomcat/*", "embedTomcat");

        tomcat.start();

        when()
                .get("http://localhost:" + portNumber + "/embedTomcat/")
        .then()
                .statusCode(200)
                .content(equalTo("Hello world from plain servlet"));

        tomcat.stop();
        tomcat.destroy();
    }

    @Test
    public void embedCXF() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setSilent(true);
        tomcat.setBaseDir(getTmpTomcatFolderName());
        int portNumber = PortUtil.getPortNumber(getClass());
        tomcat.setPort(portNumber);

        Context context = tomcat.addContext("", getTmpTomcatFolderName());
        Wrapper servlet = context.createWrapper();
        servlet.setName("embedCXF");
        servlet.setServletClass("org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet");
        servlet.addInitParameter(
                "jaxrs.serviceClasses",
                HelloWorldResource.class.getName()
        );
        context.addChild(servlet);
        context.addServletMapping( "/embedCXF/*", "embedCXF" );

        tomcat.start();

        when()
                .get("http://localhost:" + portNumber + "/embedCXF/")
        .then()
                .statusCode(200)
                .content(equalTo("Hello world from CXF"));

        tomcat.stop();
        tomcat.destroy();
    }

    @Test
    public void embedRepositoryJaxrsServlet() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setSilent(true);
        tomcat.setBaseDir(getTmpTomcatFolderName());
        int portNumber = PortUtil.getPortNumber(getClass());
        tomcat.setPort(portNumber);

        Context context = tomcat.addContext("", getTmpTomcatFolderName());
        Tomcat.addServlet(context, "embedRepositoryJaxrsServlet", new RepositoryJaxrsServlet());
        context.addServletMapping("/embedRepositoryJaxrsServlet/*", "embedRepositoryJaxrsServlet");
        tomcat.start();

        RepositoryJaxrsEndpoint jaxrsEndpoint = new CXFRepositoryJaxrsEndpoint("/")
                .invoker(new JAXRSInvoker())
                .singleton(new HelloWorldResource());
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        when()
                .get("http://localhost:" + portNumber + "/embedRepositoryJaxrsServlet/")
        .then()
                .statusCode(200)
                .content(equalTo("Hello world from CXF"));

        tomcat.stop();
        tomcat.destroy();
    }

}
