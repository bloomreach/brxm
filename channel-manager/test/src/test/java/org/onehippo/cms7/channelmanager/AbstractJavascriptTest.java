/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import org.junit.After;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

abstract public class AbstractJavascriptTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractJavascriptTest.class);

    public static final String LISTEN_HOST = "localhost";
    public static final int LISTEN_PORT = 8888;

    static class ExtJavascriptEngine extends JavaScriptEngine {

        private final ExtHtmlUnitContextFactory hucf;

        public ExtJavascriptEngine(WebClient webClient) {
            super(webClient);
            hucf = new ExtHtmlUnitContextFactory(webClient);
        }

        @Override
        public ExtHtmlUnitContextFactory getContextFactory() {
            return hucf;
        }

    }

    private static class ExtHtmlUnitContextFactory extends HtmlUnitContextFactory {
        public ExtHtmlUnitContextFactory(final WebClient webClient) {
            super(webClient);
        }

        @Override
        public net.sourceforge.htmlunit.corejs.javascript.Context makeContext() {
            net.sourceforge.htmlunit.corejs.javascript.Context context = super.makeContext();
            context.setOptimizationLevel(-1);
            return context;
        }

    }

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
        server.setConnectors(new Connector[]{connector});

        Context root = new Context(server, "/", Context.SESSIONS);
        root.setResourceBase(".");
        root.addServlet(DefaultServlet.class, "/*");
        root.addServlet(ResourceServlet.class, "/resources/*");
        root.setClassLoader(getClass().getClassLoader());
        root.getSessionHandler().getSessionManager().setSessionURL("none");

        server.start();

        WebClient client = new WebClient(BrowserVersion.FIREFOX_24);
        client.setJavaScriptEngine(new ExtJavascriptEngine(client));
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
        Window window = (Window) client.getCurrentWindow().getScriptObject();
        window.initialize(page);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

    protected void startConsole(WebClient client) {
        client.initializeEmptyWindow(client.getCurrentWindow());
        Window window = (Window) client.getCurrentWindow().getScriptObject();

        Scriptable console = (Scriptable) window.get("console");
        if (console == null) {
            ExtJavascriptEngine eje = (ExtJavascriptEngine) client.getJavaScriptEngine();
            net.sourceforge.htmlunit.corejs.javascript.Context context = eje.getContextFactory().makeContext();
            console = context.newObject(window, "Object");
            ScriptableObject.putProperty(window, "console", console);
        }

        final Function jsxLog = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    log.info((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "log", jsxLog);

        final Function jsxError = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    log.error((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "error", jsxError);

        final Function jsxWarn = new BaseFunction() {
            private static final long serialVersionUID = -2445994102698852899L;

            @Override
            public Object call(net.sourceforge.htmlunit.corejs.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length > 0 && args[0] instanceof String) {
                    log.warn((String) args[0]);
                }
                return null;
            }
        };
        ScriptableObject.putProperty(console, "warn", jsxWarn);
    }

    protected String eval(String objectIdentifier) {
        return (String) page.executeJavaScript(objectIdentifier).getJavaScriptResult();
    }

    protected void injectJavascript(Class<?> clazz, String resource) throws IOException {
        final InputStream inputStream = clazz.getResourceAsStream(resource);

        Reader resourceReader = new InputStreamReader(inputStream);
        StringBuilder javascript = new StringBuilder();
        int buffer = 0;
        try {
            while ((buffer = resourceReader.read()) != -1) {
                javascript.append((char) buffer);
            }
            evalWithScriptElement(javascript.toString());
        } finally {
            resourceReader.close();
        }
    }

    public void evalWithScriptElement(final String javascript) {
        final List<DomElement> head = page.getElementsByTagName("head");
        final DomElement script = page.createElement("script");
        script.setAttribute("type", "text/javascript");
        final Text textNode = page.createTextNode(javascript);
        script.appendChild(textNode);
        head.get(0).appendChild(script);
    }
}
