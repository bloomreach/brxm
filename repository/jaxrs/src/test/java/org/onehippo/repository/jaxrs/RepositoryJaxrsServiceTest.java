/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jayway.restassured.specification.RequestSpecification;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.RepositoryService;
import org.onehippo.repository.testutils.PortUtil;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_TYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_VALUE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_FACET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAIN;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAINRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.fail;
import static org.onehippo.repository.jaxrs.RepositoryJaxrsService.HIPPO_REST_PERMISSION;

public class RepositoryJaxrsServiceTest extends RepositoryTestCase {

    private Tomcat tomcat;
    private int portNumber;

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

    @Before
    public void setupTomcat() throws LifecycleException {
        // Setup an embedded Tomcat with a dummy servlet - Tomcat won't start if "nothing" is configured. Adding the
        // RepositoryJaxrsServlet is done through a separate call to make it possible to test adding endpoints before
        // RepositoryJaxrsService#init() is called.
        tomcat = new Tomcat();
        tomcat.setSilent(true);
        tomcat.setBaseDir(getTmpTomcatFolderName());
        portNumber = PortUtil.getPortNumber(getClass());
        tomcat.setPort(portNumber);
        Context context = tomcat.addContext("/plain", getTmpTomcatFolderName());
        Tomcat.addServlet(context, "plain", new HttpServlet() {
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                resp.setContentType("text/plain");
                Writer w = resp.getWriter();
                w.write("Hello world from plain servlet");
                w.flush();
            }
        });
        context.addServletMapping("/*", "plain");
        tomcat.start();

        if (HippoServiceRegistry.getService(RepositoryService.class) == null) {
            HippoServiceRegistry.register((RepositoryService)server.getRepository(), RepositoryService.class);
        }
    }

    @After
    public void tearDownTomcat() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }

    @Before
    public void adjustLogging() {

    }

    @After
    public void resetLogging() {

    }

    private void addEndpoint(String str) {
        addEndpoint(str, str);
    }

    private void addEndpoint(String path, String message) {
        RepositoryJaxrsEndpoint jaxrsEndpoint = new RepositoryJaxrsEndpoint("/" + path)
                .singleton(new HelloWorldResource(message));
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);
    }

    private void expectCXFOK() {
        when()
                .get("http://localhost:" + portNumber + "/META-INF/cxf/")
        .then()
                .statusCode(200)
                .content(equalTo("Hello world from CXF"));
    }

    private void expectOK(String pathAndMessage) {
        expectOK(pathAndMessage, pathAndMessage);
    }

    private void expectOK(String path, String message) {
        expectOK(SYSTEMUSER_ID, String.valueOf(SYSTEMUSER_PASSWORD), path, message);
    }

    // Passing in null for the userid will run the check without logging in
    private void expectOK(String userid, String passwd, String path, String message) {
        RequestSpecification client;
        if (userid != null) {
            client = given().auth().preemptive().basic(userid, passwd);
        } else {
            client = given();
        }
        client.get("http://localhost:" + portNumber + "/jaxrs/" + path + "/")
        .then()
                .statusCode(200)
                .content(equalTo(message));
    }

    private void expectStatusCode(String path, int statusCode) {
        expectStatusCode(SYSTEMUSER_ID, String.valueOf(SYSTEMUSER_PASSWORD), path, statusCode);
    }

    // Passing in null for the userid will run the check without logging in
    private void expectStatusCode(String userid, String passwd, String path, int statusCode) {
        Log4jInterceptor.onWarn()
            .deny(org.apache.cxf.transport.servlet.ServletController.class)
            // suppress: WARN [org.apache.http.impl.auth.HttpAuthenticator.handleAuthChallenge().167] Malformed challenge: Authentication challenge is empty
            .deny(org.apache.http.impl.client.DefaultHttpClient.class)
            .run(() -> {
                RequestSpecification client;
                if (userid != null) {
                    client = given().auth().preemptive().basic(userid, String.valueOf(passwd));
                } else {
                    client = given();
                }
                client.get("http://localhost:" + portNumber + "/jaxrs/" + path + "/")
                        .then()
                        .statusCode(statusCode);
            });
    }

    private String getTmpTomcatFolderName() {
        return tmpTomcatFolder.getRoot().getAbsolutePath();
    }

    public void initializeCXF() {
        Context context = tomcat.addContext("/META-INF/cxf", getTmpTomcatFolderName());
        Wrapper servlet = context.createWrapper();
        servlet.setName("embedCXF");
        servlet.setServletClass("org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet");
        servlet.addInitParameter(
                "jaxrs.serviceClasses",
                HelloWorldResource.class.getName()
        );
        context.addChild(servlet);
        context.addServletMapping( "/*", "embedCXF" );
    }

    private void initializeRepoJaxrsService() {
        Context context = tomcat.addContext("/jaxrs", getTmpTomcatFolderName());
        Tomcat.addServlet(context, "jaxrs", new RepositoryJaxrsServlet());
        context.addServletMapping("/*", "jaxrs");
    }

    @Test
    public void test_simplest_scenario_verbose() {
        initializeRepoJaxrsService();

        RepositoryJaxrsEndpoint jaxrsEndpoint = new RepositoryJaxrsEndpoint("/simple")
                .singleton(new HelloWorldResource("simple"));
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        given()
                .auth().preemptive().basic(SYSTEMUSER_ID, String.valueOf(SYSTEMUSER_PASSWORD))
        .when()
                .get("http://localhost:" + portNumber + "/jaxrs/simple")
        .then()
               .statusCode(200)
               .content(equalTo("simple"));
    }

    @Test
    public void test_simplest_scenario_condensed() {
        initializeRepoJaxrsService();

        addEndpoint("simple");

        expectOK("simple");
    }

    @Test
    public void test_multiple_endpoints() {
        initializeRepoJaxrsService();

        addEndpoint("one");
        addEndpoint("two");
        addEndpoint("three");

        expectOK("one");
        expectOK("two");
        expectOK("three");
    }

    @Test
    public void test_removal() {
        initializeRepoJaxrsService();

        addEndpoint("one");
        addEndpoint("two");
        addEndpoint("three");

        expectOK("one");
        expectOK("two");
        expectOK("three");

        RepositoryJaxrsService.removeEndpoint("/two");
        expectOK("one");
        expectStatusCode("two", 404);
        expectOK("three");

        RepositoryJaxrsService.removeEndpoint("/one");
        expectStatusCode("one", 404);
        expectStatusCode("two", 404);
        expectOK("three");

        RepositoryJaxrsService.removeEndpoint("/three");
        expectStatusCode("one", 404);
        expectStatusCode("two", 404);
        expectStatusCode("three", 404);
    }

    @Test
    public void test_removal_and_reregistration() {
        initializeRepoJaxrsService();

        for (int i = 0; i < 3; i++) {
            addEndpoint("one", String.valueOf(i));
            addEndpoint("two", String.valueOf(i));
            addEndpoint("three", String.valueOf(i));

            expectOK("one", String.valueOf(i));
            expectOK("two", String.valueOf(i));
            expectOK("three", String.valueOf(i));

            RepositoryJaxrsService.removeEndpoint("/one");
            RepositoryJaxrsService.removeEndpoint("/two");
            RepositoryJaxrsService.removeEndpoint("/three");

            expectStatusCode("one", 404);
            expectStatusCode("two", 404);
            expectStatusCode("three", 404);
        }
    }

    @Test
    public void expect_exception_when_registering_multiple_endpoints_on_same_path_before_initialization() {
        addEndpoint("one", "message");
        expectStatusCode("one", 404);

        try {
            addEndpoint("one", "other message");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // ok
        }

        initializeRepoJaxrsService();

        expectOK("one", "message");
    }

    @Test
    public void expect_exception_when_registering_multiple_endpoints_on_same_path_after_initialization() {
        initializeRepoJaxrsService();

        addEndpoint("one", "message");
        expectOK("one", "message");

        try {
            addEndpoint("one", "other message");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // ok
        }

        expectOK("one", "message");
    }

    @Test
    public void test_compatibility_with_cxf_servlet() {
        initializeCXF();

        expectCXFOK();

        addEndpoint("one", "message");
        expectStatusCode("one", 404);

        initializeRepoJaxrsService();

        expectOK("one", "message");

        expectCXFOK();
    }

    @Test
    public void test_authentication_required() {
        initializeRepoJaxrsService();

        addEndpoint("test");
        expectOK("test");

        // Test explicitly that when not giving any credentials, an error is returned
        expectStatusCode(null, null, "test", 401);
    }

    @Test
    public void test_incorrect_credentials_on_authenticated_endpoint() {
        initializeRepoJaxrsService();

        addEndpoint("test");
        expectOK("test");

        // Test explicitly that when giving incorrect credentials, an error is returned
        expectStatusCode("foo", "bar", "test", 401);
    }

    @Test
    public void it_must_be_possible_to_mix_authenticated_and_open_endpoints() {
        initializeRepoJaxrsService();

        // Create a "default" endpoint, requiring authentication
        RepositoryJaxrsEndpoint jaxrsEndpoint = new RepositoryJaxrsEndpoint("/auth")
                .singleton(new HelloWorldResource("auth"));
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        // Create an endpoint that does not require authentication
        jaxrsEndpoint = new CXFRepositoryJaxrsEndpoint("/open")
                .invoker(new JAXRSInvoker())
                .singleton(new HelloWorldResource("open"));
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        // Accessing the "default" endpoint should only work when giving the right credentials
        expectOK("auth");

        // And give an error when the authentication is missing or wrong
        expectStatusCode(null, null, "auth", 401);
        expectStatusCode("foo", "bar", "auth", 401);

        // And accessing the open endpoint must simply work
        expectOK(null, null, "open", "open");
    }

    @Test
    public void test_authorized_endpoint() throws RepositoryException {
        initializeRepoJaxrsService();

        // Set up two users: user1 and user2. Set up a security domain that adds the "restuser" role to all
        // nt:unstructured documents, thus giving the HIPPO_REST_PERMISSION permission on all those documents as well.
        // Only add user1 to this domain, which should have the effect that user1 can and user2 can NOT access this
        // endpoint. Last but not least add an nt:unstructured node in the root.
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        Node user = users.addNode("user1", NT_USER);
        user.setProperty(HIPPO_PASSWORD, "pwd");
        user = users.addNode("user2", NT_USER);
        user.setProperty(HIPPO_PASSWORD, "pwd");

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        final Node domain = domains.addNode("jaxrstestdomain", NT_DOMAIN);
        final Node authrole = domain.addNode("hippo:authrole", NT_AUTHROLE);
        authrole.setProperty(HippoNodeType.HIPPO_ROLE, "restuser");
        authrole.setProperty(HippoNodeType.HIPPO_USERS, new String[]{"user1"});
        final Node dr = domain.addNode("hippo:domainrule", NT_DOMAINRULE);
        final Node fr = dr.addNode("hippo:facetrule", NT_FACETRULE);
        fr.setProperty(HIPPO_FACET, "jcr:primaryType");
        fr.setProperty(HIPPOSYS_VALUE, "*");
        fr.setProperty(HIPPOSYS_TYPE, "Name");

        session.getRootNode().addNode("jaxrstestnode", "nt:unstructured");

        session.save();

        RepositoryJaxrsEndpoint jaxrsEndpoint = new RepositoryJaxrsEndpoint("/custauth")
                .authorized("/jaxrstestnode", HIPPO_REST_PERMISSION)
                .singleton(new HelloWorldResource("custauth"));
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        expectOK("user1", "pwd", "custauth", "custauth");

        expectStatusCode("user2", "pwd", "custauth", 403);

        // Clean up
        session.getNode("/hippo:configuration/hippo:users/user1").remove();
        session.getNode("/hippo:configuration/hippo:users/user2").remove();
        session.getNode("/hippo:configuration/hippo:domains/jaxrstestdomain").remove();
        session.getNode("/jaxrstestnode").remove();
        session.save();
    }

}
