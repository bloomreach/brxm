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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.google.gson.Gson;

import org.junit.Test;
import org.onehippo.cms7.channelmanager.templatecomposer.GlobalBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.onehippo.cms7.jquery.JQueryBundle;
import org.w3c.dom.Text;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import static org.junit.Assert.assertTrue;

public class InitializationTest extends AbstractChannelManagerTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: EditorManagerTest.java 29553 2011-08-15 11:15:01Z fvlankvelt $";

    @Test
    public void testMiFrameMessageHandling() throws Exception {
        setUp("test.html");

        injectJavascript(InitializationTest.class, "initMiFrameMessageMock.js");

        page.executeJavaScript("onhostmessage(function(msg) { window.initTestOK=msg.data.initTest; }, window, false, 'test');");
        page.executeJavaScript("sendMessage({initTest: true}, 'test')");

        Window window = (Window) page.getWebClient().getCurrentWindow().getScriptObject();
        assertTrue(Boolean.TRUE.equals(window.get("initTestOK")));
    }

    @Test
    public void testInitialisation() throws Exception {
        setUp("test.html");
        initializeIFrameHead();

        final Set<String> messagesSend = new HashSet<String>();

        Window window = (Window) page.getWebClient().getCurrentWindow().getScriptObject();
        final Function oldFunction = (Function) window.get("sendMessage");
        ScriptableObject.putProperty(window, "sendMessage", new BaseFunction() {
            @Override
            public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
                if (args.length >= 2 && args[1] instanceof String) {
                    messagesSend.add((String) args[1]);
                }
                return oldFunction.call(cx, scope, thisObj, args);
            }
        });

        initializeTemplateComposer(false, true);

        assertTrue(messagesSend.contains("afterinit"));
    }

    @Test
    public void testBuildOverlayXTypeHSTvBox() throws Exception {
        setUp("HST-vbox.html");
        initializeIFrameHead();
        initializeTemplateComposer(false, false);

        // test if container is present
        final List<HtmlElement> divs = page.getElementsByTagName("div");
        HtmlElement containerDiv = null;
        for (HtmlElement div : divs) {
            if (eval("HST.CLASS.CONTAINER").equals(div.getAttribute("class"))) {
                containerDiv = div;
            }
        }
        assertTrue(containerDiv != null);
        assertTrue(!isMetaDataConsumed(containerDiv));

        page.executeJavaScript("sendMessage(" +
            "{ getName: function(id) { " +
                "return (id === 'cf291fdc-d962-4c14-a5ba-3111fec861fd')? 'containerItem1' : 'containerItem2'; } " +
            "}, 'buildOverlay');");

        // test if hst meta data is consumed
        assertTrue(isMetaDataConsumed(containerDiv));

        // check if attributes are set to element
        // "xtype":"HST.vBox", "uuid":"ae12f114-9a61-47ff-b048-71852e0f2f18", "type":"CONTAINER_COMPONENT"
        assertTrue("HST.vBox".equals(containerDiv.getAttribute(eval("HST.ATTR.XTYPE"))));
        assertTrue(eval("HST.CONTAINER").equals(containerDiv.getAttribute(eval("HST.ATTR.TYPE"))));
        assertTrue("ae12f114-9a61-47ff-b048-71852e0f2f18".equals(containerDiv.getAttribute(eval("HST.ATTR.ID"))));
    }

    @Test
    public void testSurfAndEdit() throws Exception {
        setUp("surfandedit.html");
        initializeIFrameHead();
        initializeTemplateComposer(false, true);

        // test if container is present
        final List<HtmlElement> divs = page.getElementsByTagName("a");
        HtmlElement link = null;
        for (HtmlElement div : divs) {
            if (eval("HST.CLASS.EDITLINK").equals(div.getAttribute("class"))) {
                link = div;
            }
        }
        assertTrue(link != null);
        assertTrue(isMetaDataConsumed(link));
    }

    private String eval(String objectIdentifier) {
        return (String) page.executeJavaScript(objectIdentifier).getJavaScriptResult();
    }

    private boolean isMetaDataConsumed(final HtmlElement containerDiv) {
        boolean metaDataConsumed = true;
        DomNode tmp = containerDiv;
        while ((tmp = tmp.getPreviousSibling()) != null) {
            if (tmp.getNodeType() == 8) {
                metaDataConsumed = false;
            }
        }
        return metaDataConsumed;
    }

    protected void initializeIFrameHead() throws IOException {
        injectJavascript(InitializationTest.class, "initMiFrameMessageMock.js");

        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_CORE);
        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_CLASS_PLUGIN);
        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_NAMESPACE_PLUGIN);
        injectJavascript(JQueryBundle.class, JQueryBundle.JQUERY_UI);

        injectJavascript(GlobalBundle.class, GlobalBundle.GLOBALS);
        injectJavascript(IFrameBundle.class, IFrameBundle.UTIL);
        injectJavascript(IFrameBundle.class, IFrameBundle.SURFANDEDIT);
        injectJavascript(IFrameBundle.class, IFrameBundle.MANAGER);
        injectJavascript(IFrameBundle.class, IFrameBundle.FACTORY);
        injectJavascript(IFrameBundle.class, IFrameBundle.WIDGETS);
        injectJavascript(IFrameBundle.class, IFrameBundle.MAIN);
    }

    protected void initializeTemplateComposer(final Boolean debug, final Boolean previewMode) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(PageEditor.class.getName());
        final Map<String, String> resourcesMap = new HashMap<String, String>();
        for (String key : resourceBundle.keySet()) {
            resourcesMap.put(key, resourceBundle.getString(key));
        }

        Map<String, Object> argument = new HashMap<String, Object>();
        argument.put("debug", debug);
        argument.put("previewMode", previewMode);
        argument.put("resources", resourcesMap);

        Gson gson = new Gson();
        String message = gson.toJson(argument);

        page.executeJavaScript("sendMessage("+message+", 'init');");
    }

    protected void injectJavascript(Class<?> clazz, String resource) throws IOException {
        final InputStream inputStream = clazz.getResourceAsStream(resource);

        Reader resourceReader = new InputStreamReader(inputStream);
        StringBuilder javascript = new StringBuilder();
        int buffer = 0;
        try {
            while ((buffer = resourceReader.read()) != -1) {
                javascript.append((char)buffer);
            }
            final List<HtmlElement> head = page.getElementsByTagName("head");
            final HtmlElement script = page.createElement("script");
            script.setAttribute("type", "text/javascript");
            final Text textNode = page.createTextNode(javascript.toString());
            script.appendChild(textNode);
            head.get(0).appendChild(script);
        } finally {
            resourceReader.close();
        }
    }

}
