/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.jaxrs.cxf;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.specification.RequestSender;
import com.jayway.restassured.specification.RequestSpecification;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.junit.After;
import org.onehippo.repository.testutils.PortUtil;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Utility class for unit testing of JAXRS endpoints; for examples how to use this class, see the unit tests.
 *
 * <p>The smallest example can be found in the test class {@code org.onehippo.jaxrs.cxf.TestSingleEndpoint}. This
 * tests a single endpoint. It is possible to test more complex setups, for instance using multiple endpoints or using
 * custom object mappers. To create such a setup, first call {@link #createDefaultConfig()} or
 * {@link #createDefaultConfig(Class<?>)} and add additional JAXRS endpoints, filters, providers, etc. Subsequently
 * call {@link #setup(Config)} to create your test fixture.</p>
 *
 * <p>Most examples use <a href="https://github.com/jayway/rest-assured">REST-assured</a> to write the test conditions.
 * Note that this class provides {@link #given()} and {@link #when()} methods that return an initialized REST-assured
 * "client", connected to the test fixture.
 * See their <a href="https://github.com/jayway/rest-assured">GitHub page</a> or
 * <a href="https://github.com/jayway/rest-assured/wiki/Usage">user guide</a> for more information. Be sure to check
 * the <a href="https://github.com/jayway/rest-assured/wiki/Usage#examples">examples</a> section.</p>
 *
 * <p>Next to testing of the plain JSON output, it is also possible to test using a JAXRS client, see the test class
 * {@code org.onehippo.jaxrs.cxf.TestJaxrsClient}.</p>
 *
 * <p>A note on thread-safety: it is possible to run tests that extend from CXFTest multi-threaded. Be sure to use
 * {@link #given()} or {@link #when()} to create a REST-assured client, other mechanisms of initializing
 * REST-assured may not be thread-safe.</p>
 *
 * <p>In general the CXFTest testbed has been tested to be compatible with the following frameworks found in the Hippo
 * stack:
 * <ul>
 *     <li>
 *         HST: if your code uses {@code HstRequestContext}, it is possible to set a mocked {@code HstRequestContext}
 *         in the thread local storage using a helper class. See
 *         {@code org.onehippo.jaxrs.cxf.hst.TestHstTestFixtureHelper} in the HST project how to do this.
 *     </li>
 *     <li>
 *         Spring: see test class {@code org.onehippo.jaxrs.cxf.TestCompatibilityWithSpring}
 *     </li>
 *     <li>
 *         PowerMock: see test class {@code org.onehippo.jaxrs.cxf.TestCompatibilityWithPowerMock}, do note the three
 *         class level annotations ({@literal @RunWith(PowerMockRunner.class) @PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
 *         @PrepareForTest(Class<?>)}) that are all necessary to make the test run successfully.
 *     </li>
 * </ul>
 * </p>
 */
public class CXFTest {

    private Class<?> defaultSerializer = JacksonJsonProvider.class;

    private String address;
    private Server server;
    private Set<Class<?>> clientClasses;
    private Set<Object> clientSingletons;

    protected Class<?> getDefaultSerializer() {
        return defaultSerializer;
    }

    @SuppressWarnings("unused")
    protected void setDefaultSerializer(Class<?> defaultSerializer) {
        this.defaultSerializer = defaultSerializer;
    }

    @SuppressWarnings("unused")
    protected Builder createJaxrsClient() {
        return createJaxrsClient("", APPLICATION_JSON);
    }

    protected Builder createJaxrsClient(final String url) {
        return createJaxrsClient(url, APPLICATION_JSON);
    }

    protected Builder createJaxrsClient(final String url, final String mediaType) {
        Client client = ClientBuilder.newClient();
        for (Class<?> cls: clientClasses) {
            client.register(cls);
        }
        for (Object obj: clientSingletons) {
            client.register(obj);
        }
        return client.target(address).path(url).request(mediaType);
    }

    /** Utility method to create a REST-assured "client" in a thread-safe manner.
     * @return An initialized {@link RequestSpecification} connected to this test fixture.
     */
    protected RequestSpecification given() {
        return RestAssured.given().proxy(getServerHost(), getServerPort());
    }

    /** Utility method to create a REST-assured "client" in a thread-safe manner.
     * @return An initialized {@link RequestSender} connected to this test fixture.
     */
    protected RequestSender when() {
        return this.given().when();
    }

    public class Config {
        private final Set<Class<?>> clientClasses = new HashSet<>();
        private final Set<Object>   clientSingletons = new HashSet<>();
        private final Set<Class<?>> serverClasses = new HashSet<>();
        private final Set<Object>   serverSingletons = new HashSet<>();

        public Config addClientClass(final Class<?> clientClass) {
            clientClasses.add(clientClass);
            return this;
        }
        @SuppressWarnings("unused")
        public Config addClientSingleton(final Object singleton) {
            clientSingletons.add(singleton);
            return this;
        }
        public Config addServerClass(final Class<?> serverClass) {
            serverClasses.add(serverClass);
            return this;
        }
        public Config addServerSingleton(final Object singleton) {
            serverSingletons.add(singleton);
            return this;
        }
        public Set<Class<?>> getClientClasses() {
            return clientClasses;
        }
        public Set<Object> getClientSingletons() {
            return clientSingletons;
        }
        public Set<Class<?>> getServerClasses() {
            return serverClasses;
        }
        public Set<Object> getServerSingletons() {
            return serverSingletons;
        }
    }

    protected Config createDefaultConfig() {
        return new Config()
                .addClientClass(getDefaultSerializer())
                .addServerClass(getDefaultSerializer());
    }

    @SuppressWarnings("unused")
    protected Config createDefaultConfig(Class<?> objectMapper) {
        return createDefaultConfig()
                .addClientClass(objectMapper)
                .addServerClass(objectMapper);
    }

    protected void setup(Object endpointSingleton) {
        Config config = createDefaultConfig()
                .addServerSingleton(endpointSingleton);
        setup(config);
    }

    protected void setup(Class<?> endpointClass) {
        Config config = createDefaultConfig()
                .addServerClass(endpointClass);
        setup(config);
    }

    protected String getServerHost() {
        return "localhost";
    }

    protected int getServerPort() {
        return PortUtil.getPortNumber(getClass());
    }

    protected String getServerAddress() {
        return "http://" + getServerHost() + ":" + getServerPort();
    }

    protected void setup(Config config) {
        address = getServerAddress();

        Application application = new Application() {
            public Set<Class<?>> getClasses() {
                return config.getServerClasses();
            }

            public Set<Object> getSingletons() {
                return config.getServerSingletons();
            }
        };
        JAXRSServerFactoryBean serverFactory = ResourceUtils.createApplication(application, true);
        serverFactory.setAddress(address);

        server = serverFactory.create();
        server.start();

        clientClasses = config.getClientClasses();
        clientSingletons = config.getClientSingletons();
    }

    @After
    public void tearDownBackend() {
        if (server != null) {
            server.destroy();
            server = null;
        }
    }
}
