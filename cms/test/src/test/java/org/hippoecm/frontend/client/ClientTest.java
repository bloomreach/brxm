/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.client;

import java.io.IOException;
import java.net.URL;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.hippoecm.frontend.Main;
import org.junit.After;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

abstract public class ClientTest {

    static final Logger log = LoggerFactory.getLogger(ClientTest.class);

    public static final String LISTEN_HOST = "localhost";
    public static final int LISTEN_PORT = 8888;

    Server server;
    protected HtmlPage page;

    public void setUp(String application) throws Exception {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(LISTEN_HOST);
        connector.setPort(LISTEN_PORT);
        server.setConnectors(new Connector[] { connector });
        Context root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(DefaultServlet.class, "/*");
        FilterHolder filterHolder = new FilterHolder(WicketFilter.class);
        filterHolder.setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, Main.class.getName());
        if (application != null) {
            filterHolder.setInitParameter("config", application);
        }
        root.addFilter(filterHolder, "/*", 1);
        server.start();

        WebClient client = new WebClient();
        client.setAjaxController(new AjaxController() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean processSynchron(HtmlPage page, WebRequestSettings settings, boolean async) {
                return true;
            }
        });

        final WebConnection upstreamConnection = client.getWebConnection();
        client.setWebConnection(new WebConnection() {

            public WebResponse getResponse(WebRequestSettings requestSettings) throws IOException {
                WebResponse response = upstreamConnection.getResponse(requestSettings);
//                System.out.println(response.getContentAsString());
                return response;
            }

        });

        client.initializeEmptyWindow(client.getCurrentWindow());
        Window window = (Window) client.getCurrentWindow().getScriptObject();
        final Function jsLogger = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope,
                    Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    System.out.println((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(window, "log", jsLogger);
        WebWindow testWindow = client.openWindow(new URL("http://localhost:" + LISTEN_PORT), LISTEN_HOST);

        page = (HtmlPage) testWindow.getEnclosedPage();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

}
