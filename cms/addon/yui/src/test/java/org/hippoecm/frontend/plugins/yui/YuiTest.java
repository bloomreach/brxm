/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.yui;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.junit.After;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;

abstract public class YuiTest {

    public static final String LISTEN_HOST = "localhost";
    public static final int LISTEN_PORT = 8888;

    static final String WICKET_WEBAPP_CLASS_NAME = YuiWicketApplication.class.getName();

    Server server;

    public void setUp(Class<? extends WebPage> clazz) throws Exception {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(LISTEN_HOST);
        connector.setPort(LISTEN_PORT);
        server.setConnectors(new Connector[] { connector });
        Context root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(DefaultServlet.class, "/*");
        FilterHolder filterHolder = new FilterHolder(WicketFilter.class);
        filterHolder.setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, WICKET_WEBAPP_CLASS_NAME);
        filterHolder.setInitParameter("test-page-class", clazz.getName());
        root.addFilter(filterHolder, "/*", 1);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

}
