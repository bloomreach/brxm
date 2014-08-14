/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
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

@Provider
public class CXFApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(CXFApplication.class);

    private Set<Object> dummySingletons = new HashSet<Object>(Arrays.asList(new DummyService()));
    private Set<Object> singletons;
    private volatile int version = -1;

    public CXFApplication() {
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
        final Set<Object> singletons = new HashSet<Object>();
        final List<HippoServiceRegistration> registrations = getServiceRegistrations();
        for (HippoServiceRegistration registration : registrations) {
            final Object service = registration.getService();
            log.debug("Found singleton JaxrsService " + service);
            singletons.add(service);
        }
        this.singletons = singletons;
        version = HippoServiceRegistry.getVersion();
    }

    protected List<HippoServiceRegistration> getServiceRegistrations() {
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
