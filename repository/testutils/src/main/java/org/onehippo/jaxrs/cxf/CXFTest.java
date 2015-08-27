/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.junit.After;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
    protected Builder createClient() {
        return createClient("", APPLICATION_JSON);
    }

    protected Builder createClient(final String url) {
        return createClient(url, APPLICATION_JSON);
    }

    protected Builder createClient(final String url, final String mediaType) {
        Client client = ClientBuilder.newClient();
        for (Class<?> cls: clientClasses) {
            client.register(cls);
        }
        for (Object obj: clientSingletons) {
            client.register(obj);
        }
        return client.target(address).path(url).request(mediaType);
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
    
    protected void setup(Object endpointSingleton) {
        Config config = createDefaultConfig()
                .addServerSingleton(endpointSingleton);
        setup(config);
    }

    protected void setup(Object endpointSingleton, Class<?> objectMapper) {
        Config config = createDefaultConfig()
                .addServerSingleton(endpointSingleton)
                .addServerClass(objectMapper)
                .addClientClass(objectMapper);
        setup(config);
    }

    protected void setup(Class<?> endpointClass) {
        Config config = createDefaultConfig()
                .addServerClass(endpointClass);
        setup(config);
    }

    protected void setup(Class<?> endpointClass, Class<?> objectMapper) {
        Config config = createDefaultConfig()
                .addServerClass(endpointClass)
                .addServerClass(objectMapper)
                .addClientClass(objectMapper);
        setup(config);
    }

    protected String getServerHost() {
        return "http://localhost";
    }

    protected String getServerPort() {
        return TestUtil.getPortNumber(getClass());
    }

    protected String getServerAddress() {
        return getServerHost() + ":" + getServerPort();
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
        JAXRSServerFactoryBean endpointFactory = ResourceUtils.createApplication(application, true);
        endpointFactory.setAddress(address);

        server = endpointFactory.create();
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
