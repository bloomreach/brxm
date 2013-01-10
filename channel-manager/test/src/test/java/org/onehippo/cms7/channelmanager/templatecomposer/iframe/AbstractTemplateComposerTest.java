/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import net.sourceforge.htmlunit.corejs.javascript.BaseFunction;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.onehippo.cms7.channelmanager.AbstractJavascriptTest;
import org.onehippo.cms7.channelmanager.templatecomposer.TemplateComposerGlobalBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.onehippo.cms7.jquery.JQueryBundle;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.javascript.host.Node;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.google.gson.Gson;

abstract public class AbstractTemplateComposerTest extends AbstractJavascriptTest {

    public class Message {
        public String messageTag;
        public Object messagePayload;

        public Message(final String messageTag, final Object messagePayload) {
            this.messageTag = messageTag;
            this.messagePayload = messagePayload;
        }
    }

    private List<Message> messagesSend = new ArrayList<Message>();

    @Override
    public void setUp(String name) throws Exception {
        super.setUp(name);
        initializeIFrameHead();
    }

    protected boolean isMetaDataConsumed(final HtmlElement containerDiv) {
        boolean metaDataConsumed = true;
        DomNode tmp = containerDiv;
        while ((tmp = tmp.getPreviousSibling()) != null) {
            if (tmp.getNodeType() == Node.COMMENT_NODE) {
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

        injectJavascript(TemplateComposerGlobalBundle.class, TemplateComposerGlobalBundle.GLOBALS);
        injectJavascript(IFrameBundle.class, IFrameBundle.MAIN);
        injectJavascript(IFrameBundle.class, IFrameBundle.UTIL);
        injectJavascript(IFrameBundle.class, IFrameBundle.FACTORY);
        injectJavascript(IFrameBundle.class, IFrameBundle.PAGE);
        injectJavascript(IFrameBundle.class, IFrameBundle.WIDGETS);
        injectJavascript(IFrameBundle.class, IFrameBundle.SURFANDEDIT);

        page.executeJavaScript("jQuery.noConflict(true);");

        Window window = (Window) page.getWebClient().getCurrentWindow().getScriptObject();
        final Function oldFunction = (Function) window.get("sendMessage");
        ScriptableObject.putProperty(window, "sendMessage", new BaseFunction() {
            @Override
            public Object call(final net.sourceforge.htmlunit.corejs.javascript.Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
                AbstractTemplateComposerTest.this.messagesSend.add(new Message((String)args[1], args[0]));
                return oldFunction.call(cx, scope, thisObj, args);
            }
        });
    }

    protected void initializeTemplateComposer(final boolean debug, final boolean previewMode) {
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

        page.executeJavaScript("sendMessage(" + message + ", 'init');");
    }

    public boolean isMessageSend(final String message) {
        for (Message messageObject : this.messagesSend) {
            if (message.equals(messageObject.messageTag)) {
                return true;
            }
        }
        return false;
    }

    public void clearMessagesSend() {
        this.messagesSend.clear();
    }

    public List<Message> getMessagesSend() {
        return this.messagesSend;
    }

    public List<Message> getMessages(String message) {
        List<Message> messages = new ArrayList<Message>();
        for (Message messageObject : this.messagesSend) {
            if (message.equals(messageObject.messageTag)) {
                messages.add(messageObject);
            }
        }
        return messages;
    }

}
