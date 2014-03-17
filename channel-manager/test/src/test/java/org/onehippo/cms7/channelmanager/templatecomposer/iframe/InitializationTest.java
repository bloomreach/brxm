/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.iframe;

import java.util.List;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InitializationTest extends AbstractTemplateComposerTest {

    @Test
    public void testHostToIFramePubSub() throws Exception {
        setUp("test.html");

        page.executeJavaScript("var testListener = function(msg) { initTestOK=msg.initTest; };");
        page.executeJavaScript("Ext.getCmp('pageEditorIFrame').hostToIFrame.subscribe('test', testListener, window);");
        page.executeJavaScript("Ext.getCmp('pageEditorIFrame').hostToIFrame.publish('test', {initTest: true})");

        Window window = (Window) page.getWebClient().getCurrentWindow().getScriptObject();
        assertTrue(Boolean.TRUE.equals(window.get("initTestOK")));

        page.executeJavaScript("Ext.getCmp('pageEditorIFrame').hostToIFrame.unsubscribe('test', testListener, window);");
        page.executeJavaScript("Ext.getCmp('pageEditorIFrame').hostToIFrame.publish('test', {initTest: false})");

        assertTrue(Boolean.TRUE.equals(window.get("initTestOK")));
    }

    @Test
    public void testInitialisation() throws Exception {
        setUp("test.html");
        initializeTemplateComposer(false, true);
        assertFalse(isPublished(hostToIFrameMessages, "exception"));
    }

    @Test
    public void testBuildOverlayXTypeHSTvBox() throws Exception {
        setUp("HST-vbox.html");
        initializeTemplateComposer(false, false);

        // test if container is present
        final List<DomElement> divs = page.getElementsByTagName("div");
        DomElement containerDiv = null;
        for (DomElement div : divs) {
            if (eval("HST.CLASS.CONTAINER").equals(div.getAttribute("class"))) {
                containerDiv = div;
            }
        }
        assertNotNull(containerDiv);
        assertFalse(isMetaDataConsumed(containerDiv));

        page.executeJavaScript("Ext.getCmp('pageEditorIFrame').hostToIFrame.publish('buildoverlay');");
        assertFalse(isPublished(hostToIFrameMessages, "exception"));

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

        initializeTemplateComposer(false, false);

        assertFalse(isPublished(hostToIFrameMessages, "exception"));
    }

}
