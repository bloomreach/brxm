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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrapping servlet for dynamic rest end points.
 *
 * @version "$Id$"
 */
public class RestServlet extends CXFServlet {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(RestServlet.class);

    @Override
    public void init(final ServletConfig sc) throws ServletException {
        super.init(sc);
        //############################################
        // SCAN PLUGIN CLASSES AND REGISTER END POINTS
        //############################################
        final RuntimeDelegate delegate = RuntimeDelegate.getInstance();
        final JAXRSServerFactoryBean bean = delegate.createEndpoint(new EndpointsRestApplication(), JAXRSServerFactoryBean.class);
        final Bus bus = getBus();
        bean.setBus(bus);
        final Server server = bean.create();
        server.start();


    }
}
