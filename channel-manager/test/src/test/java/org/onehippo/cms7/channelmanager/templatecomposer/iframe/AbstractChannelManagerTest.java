package org.onehippo.cms7.channelmanager.templatecomposer.iframe;/*
 *  Copyright 2011 Hippo.
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

import java.net.URL;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import org.junit.After;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.thread.QueuedThreadPool;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

abstract public class AbstractChannelManagerTest {
    public static final String LISTEN_HOST = "localhost";
    public static final int LISTEN_PORT = 8888;

    Server server;
    protected HtmlPage page;

    public void setUp(String name) throws Exception {
        server = new Server();
        
        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMinThreads(8);
        server.setThreadPool(pool);
        
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(LISTEN_HOST);
        connector.setPort(LISTEN_PORT);
        server.setConnectors(new Connector[] { connector });
        
        Context root = new Context(server, "/", Context.SESSIONS);
        root.setResourceBase(".");
        root.addServlet(DefaultServlet.class, "/*");
        root.getSessionHandler().getSessionManager().setSessionURL("none");

        server.start();

        WebClient client = new WebClient(BrowserVersion.FIREFOX_3);
        client.setAjaxController(new AjaxController() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean processSynchron(final HtmlPage page, final WebRequest request, final boolean async) {
                return true;
            }
        });
        WebWindow testWindow = client.openWindow(new URL("http://localhost:" + LISTEN_PORT + "/" + name), LISTEN_HOST);
        startConsole(client);
        page = (HtmlPage) testWindow.getEnclosedPage();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

    void startConsole(WebClient client) {
        client.initializeEmptyWindow(client.getCurrentWindow());
        Window window = (Window) client.getCurrentWindow().getScriptObject();

        Scriptable console = (Scriptable) window.get("console");
        if (console == null) {
            net.sourceforge.htmlunit.corejs.javascript.Context context = net.sourceforge.htmlunit.corejs.javascript.Context.enter();
            console = context.newObject(window, "Object");
            ScriptableObject.putProperty(window, "console", console);
        }

        final Function jsxLog = new BaseFunction() {
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
        ScriptableObject.putProperty(console, "log", jsxLog);

        final Function jsxError = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope,
                    Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    System.err.println((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "error", jsxError);

        final Function jsxWarn = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope,
                    Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    System.err.println((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "warn", jsxWarn);
    }

}
