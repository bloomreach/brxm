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

import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import org.junit.Test;

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
        initializeTemplateComposer(false, true);

        assertTrue(!isMessageSend("iframeexception"));
        assertTrue(isMessageSend("afterinit"));
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

        assertTrue(!isMessageSend("iframeexception"));

        // test if hst meta data is consumed
        assertTrue(isMetaDataConsumed(containerDiv));

        // check if attributes are set to element
        // "xtype":"HST.vBox", "uuid":"ae12f114-9a61-47ff-b048-71852e0f2f18", "type":"CONTAINER_COMPONENT"
        assertTrue("HST.vBox".equals(containerDiv.getAttribute(eval("HST.ATTR.XTYPE"))));
        assertTrue(eval("HST.CONTAINER").equals(containerDiv.getAttribute(eval("HST.ATTR.TYPE"))));
        assertTrue("ae12f114-9a61-47ff-b048-71852e0f2f18".equals(containerDiv.getAttribute(eval("HST.ATTR.ID"))));
    }

    @Test
    public void testEmptyContainerItem() throws Exception {
        setUp("emptycontaineritem.html");

        initializeIFrameHead();
        initializeTemplateComposer(false, false);

        assertTrue(!isMessageSend("iframeexception"));
    }

}
