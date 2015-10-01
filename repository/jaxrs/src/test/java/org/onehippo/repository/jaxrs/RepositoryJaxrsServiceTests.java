/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.RepositoryService;
import org.onehippo.repository.testutils.PortUtil;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

public class RepositoryJaxrsServiceTests extends RepositoryTestCase {

    @Rule
    public TemporaryFolder tmpTomcatFolder = new TemporaryFolder();

    private String getTmpTomcatFolderName() {
        return tmpTomcatFolder.getRoot().getAbsolutePath();
    }

    @Test
    public void runRepoTestCase() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(getTmpTomcatFolderName());
        int portNumber = PortUtil.getPortNumber(getClass());
        tomcat.setPort(portNumber);

        Context context = tomcat.addContext("", getTmpTomcatFolderName());
        Tomcat.addServlet(context, "repo", new RepositoryJaxrsServlet());
        context.addServletMapping("/repo/*", "repo");
        tomcat.start();

        HippoServiceRegistry.registerService(server.getRepository(), RepositoryService.class);

        RepositoryJaxrsEndpoint jaxrsEndpoint = new RepositoryJaxrsEndpoint("/")
                .singleton(new HelloWorldResource());
        RepositoryJaxrsService.addEndpoint(jaxrsEndpoint);

        given()
                .auth().preemptive().basic(SYSTEMUSER_ID, String.valueOf(SYSTEMUSER_PASSWORD))
        .when()
                .get("http://localhost:" + portNumber + "/repo/")
        .then()
                .statusCode(200)
                .content(equalTo("Hello world from CXF"));

        tomcat.stop();
        tomcat.destroy();
    }
}
