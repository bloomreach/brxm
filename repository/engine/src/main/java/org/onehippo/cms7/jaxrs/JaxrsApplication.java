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
package org.onehippo.cms7.jaxrs;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.jaxrs.JaxrsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class consumes the {@link JaxrsService}s and exposes them as resources in a JAX-RS application.
 */
@Provider
public class JaxrsApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JaxrsApplication.class);

    private Set<Object> dummySingletons = Collections.unmodifiableSet(new HashSet<Object>() {{ add(new DummyService()); }});
    private Set<Object> singletons;
    private Set<Class<?>> classes = Collections.unmodifiableSet(new HashSet<Class<?>>() {{ add(JaxrsAuthenticationHandler.class); }});
    private volatile int version = -1;

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        if (updateSingletonsNeeded()) {
            updateSingletons();
        }
        if (singletons.isEmpty()) {
            return dummySingletons;
        }
        return singletons;
    }

    private boolean updateSingletonsNeeded() {
        return version != HippoServiceRegistry.getVersion();
    }

    private void updateSingletons() {
        final Set<Object> singletons = new HashSet<>();
        final List<HippoServiceRegistration> registrations = getJaxrsServiceRegistrations();
        for (HippoServiceRegistration registration : registrations) {
            final Object service = registration.getService();
            log.debug("Found singleton JaxrsService " + service);
            singletons.add(service);
        }
        this.singletons = Collections.unmodifiableSet(singletons);
        version = HippoServiceRegistry.getVersion();
    }

    protected List<HippoServiceRegistration> getJaxrsServiceRegistrations() {
        return HippoServiceRegistry.getRegistrations(JaxrsService.class);
    }

    @Path("/dummy")
    public class DummyService {
        @GET
        public Response getResponse() {
            return Response.status(200).build();
        }
    }
}
