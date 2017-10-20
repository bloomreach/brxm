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

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RepositoryJaxrsService provides functionality to dynamic add and remove JAX-RS REST application endpoints from
 * the CMS webapp.
 *
 * <p>These application endpoints can be accessed through a corresponding {@link RepositoryJaxrsServlet} which by
 * default is configured in the Hippo CMS application's web.xml making the application endpoints available under
 * <code>"/ws/&lt;address&gt;"</code>. This servlet
 * loads and initializes the service.</p>
 *
 * <p>To create application endpoints, use the fluent APIs of either {@link RepositoryJaxrsEndpoint} or
 * {@link CXFRepositoryJaxrsEndpoint} to create an application endpoint and then call
 * {@link RepositoryJaxrsService#addEndpoint(RepositoryJaxrsEndpoint)} to add them to the service. By default
 * application endpoints are secured with basic authentication via the Hippo Repository, i.e. require a valid
 * Repository (CMS) username and password. In addition authorization restrictions can be configured for an application
 * endpoint.</p>
 *
 * <p>For more detailed documentation and example usage, see the
 * <a href="http://www.onehippo.org/library/concepts/hippo-services/repository-jaxrs-service.html">online
 * documentation</a>.</p>
 *
 * <p>To avoid startup race conditions, it is possible to add/remove application endpoints both before and after
 * {@link RepositoryJaxrsService#init(ServletConfig, Map)} is called by {@link RepositoryJaxrsServlet} to initialize
 * the service. Application endpoints added before {@link RepositoryJaxrsService#init(ServletConfig, Map)} are
 * collected and initialized as part of {@link RepositoryJaxrsService#init(ServletConfig, Map)}.</p>
 */
public final class RepositoryJaxrsService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryJaxrsService.class);

    /**
     * Hippo permission that can be used in combination with {@link AuthorizingRepositoryJaxrsInvoker} to enforce
     * additional authorization restrictions.
     */
    public static final String HIPPO_REST_PERMISSION = "hippo:rest";

    private static RepositoryJaxrsService INSTANCE = new RepositoryJaxrsService();
    private static Bus bus;
    private static HTTPTransportFactory destinationFactory;
    private static Map<String, RepositoryJaxrsEndpoint> pendingEndpoints = new LinkedHashMap<>();
    private static Map<String, Server> servers = new HashMap<>();
    private static JAXRSInvoker jaxrsInvoker = new AuthenticatingRepositoryJaxrsInvoker();
    private static ServletController controller;

    private static class ServletConfigWrapper implements ServletConfig {

        private final ServletConfig config;
        private Map<String,String> params;

        public ServletConfigWrapper(ServletConfig config, Map<String,String> properties) {
            this.config = config;
            this.params = new HashMap<>();
            for (Enumeration<String> e = config.getInitParameterNames(); e.hasMoreElements();) {
                String param = e.nextElement();
                params.put(param, config.getInitParameter(param));
            }
            if (properties != null) {
                params.putAll(properties);
            }
            // ensure this always
            params.put("disable-address-updates", "true");
        }

        public String getInitParameter(String name) {
            return params.get(name);
        }
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(params.keySet());
        }
        public ServletContext getServletContext() {
            return config.getServletContext();
        }
        public String getServletName() {
            return config.getServletName();
        }
    }

    private RepositoryJaxrsService() {}

    /**
     * Called by the {@link RepositoryJaxrsServlet} to initialize the service and any endpoints that were added to
     * the service prior to the {@link RepositoryJaxrsService#init(ServletConfig, Map)} call.
     */
    public synchronized static RepositoryJaxrsService init(ServletConfig config, Map<String, String> properties) {
        if (bus != null) {
            throw new IllegalStateException("RepositoryJaxrsService already started");
        }
        try {
            bus = BusFactory.newInstance().createBus();
            destinationFactory = new HTTPTransportFactory();
            for (String address : pendingEndpoints.keySet()) {
                try {
                    addEndpoint(pendingEndpoints.get(address));
                }
                catch (Exception e) {
                    log.error("Failed to register endpoint "+address, e);
                }
            }
            pendingEndpoints.clear();
            controller = new ServletController(destinationFactory.getRegistry(),
                    new ServletConfigWrapper(config, properties),
                    new ServiceListGeneratorServlet(destinationFactory.getRegistry(), bus));
            return INSTANCE;
        }
        catch (Exception e) {
            INSTANCE.destroy();
            throw e;
        }
    }

    /**
     * Adds a new application endpoint. In case the service is not initialized yet, the application endpoints are
     * collected and initialized as part of {@link RepositoryJaxrsService#init(ServletConfig, Map)}.
     *
     * @param endpoint application endpoint that must be added
     * @throws IllegalStateException in case an endpoint with the same address was added in a previous call
     */
    public synchronized static void addEndpoint(final RepositoryJaxrsEndpoint endpoint) {
        String address = endpoint.getAddress();
        if (bus == null) {
            if (pendingEndpoints.containsKey(address)) {
                throw new IllegalStateException("Endpoint address " + address + " already registered.");
            }
            pendingEndpoints.put(address, endpoint);
        }
        else {
            if (servers.containsKey(address)) {
                throw new IllegalStateException("Endpoint address " + address + " already registered.");
            }

            Application app = endpoint.getApplication();
            if (app == null) {
                app = new Application() {
                    public Set<Class<?>> getClasses() {
                        return endpoint.getClasses();
                    }

                    public Set<Object> getSingletons() {
                        return endpoint.getSingletons();
                    }
                };
            }
            JAXRSServerFactoryBean endpointFactory = ResourceUtils.createApplication(app, true, false, false, bus);
            endpointFactory.setAddress(address);
            endpointFactory.setDestinationFactory(destinationFactory);

            // don't print entire stacktraces
            final WebApplicationExceptionLogger webApplicationExceptionLogger = new WebApplicationExceptionLogger();
            webApplicationExceptionLogger.setPrintStackTrace(false);
            endpointFactory.setProvider(webApplicationExceptionLogger);

            CXFRepositoryJaxrsEndpoint cxfEndpoint =
                    endpoint instanceof CXFRepositoryJaxrsEndpoint ? (CXFRepositoryJaxrsEndpoint)endpoint : null;

            JAXRSInvoker invoker = cxfEndpoint != null ? cxfEndpoint.getInvoker() : null;
            
            if (invoker == null) {
                if (endpoint.getAuthorizationNodePath() == null) {
                    invoker = jaxrsInvoker;
                }
                else {
                    invoker = new AuthorizingRepositoryJaxrsInvoker(endpoint.getAuthorizationNodePath(),
                            endpoint.getAuthorizationPermission());
                }
            }
            endpointFactory.setInvoker(invoker);

            if (cxfEndpoint != null) {
                cxfEndpoint.preCreate(endpointFactory);
            }

            Server server = endpointFactory.create();

            if (cxfEndpoint != null) {
                cxfEndpoint.postCreate(server);
            }

            servers.put(endpointFactory.getAddress(), server);
        }
    }

    /**
     * Removes the application endpoint at the given address.
     *
     * @param address address of the application endpoint that must be removed
     */
    public synchronized static void removeEndpoint(String address) {
        Server server = servers.remove(address);
        if (server != null) {
            server.destroy();
        }
        else {
            pendingEndpoints.remove(address);
        }
    }

    public void invoke(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Bus origBus = null;
        try {
            origBus = BusFactory.getAndSetThreadDefaultBus(bus);
            controller.invoke(req, resp);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
        }
    }

    public void destroy() {
        synchronized (getClass()) {
            if (bus != null) {
                try {
                    for (Server server : servers.values()) {
                        server.destroy();
                    }
                    if (destinationFactory != null) {
                        DestinationRegistry destinationRegistry = destinationFactory.getRegistry();
                        for (String path : destinationRegistry.getDestinationsPaths()) {
                            AbstractHTTPDestination dest = destinationRegistry.getDestinationForPath(path);
                            synchronized (dest) {
                                destinationRegistry.removeDestination(path);
                                dest.releaseRegistry();
                            }
                        }
                    }
                    bus.shutdown(true);
                }
                finally {
                    controller = null;
                    servers.clear();
                    pendingEndpoints.clear();
                    destinationFactory = null;
                    bus = null;
                }
            }
        }
    }


    private static class WebApplicationExceptionLogger extends WebApplicationExceptionMapper {

        @Override
        public Response toResponse(final WebApplicationException exception) {
            if (log.isDebugEnabled()) {
                log.warn(exception.toString(), exception);
            } else {
                log.warn(exception.toString());
            }
            return super.toResponse(exception);
        }
    }
}
