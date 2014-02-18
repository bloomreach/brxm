/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.servlet;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.rest.model.RestList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/endpoints")
public class EndPointList {

    private static Logger log = LoggerFactory.getLogger(EndPointList.class);

    @GET
    public RestfulList<MessageRestful> list() {
        log.info("@@@@ LISTING REST ENDPOINTS @@@@");
        final RestfulList<MessageRestful> endpoints = new RestList<>();

        try {

            final Bus bus = BusFactory.getDefaultBus();
            final DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            final DestinationFactory df = dfm.getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            if (df instanceof HTTPTransportFactory) {
                HTTPTransportFactory transportFactory = (HTTPTransportFactory) df;
                final DestinationRegistry registry = transportFactory.getRegistry();
                final Collection<AbstractHTTPDestination> destinations = registry.getDestinations();
                for (AbstractHTTPDestination destination : destinations) {
                    final String endpoint = destination.getPath();
                    final MessageRestful message = new MessageRestful(endpoint);
                    endpoints.add(message);

                }
            }
        } catch (BusException e) {

            log.error("e {}", e);
        }
        return endpoints;
    }

}
