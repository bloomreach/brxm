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

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

public class ExtTest extends AbstractJavascriptTest {

    @Test
    public void runBasicExtTest() throws Exception {
        setUp("extjs-test.html");

        // TODO: replace by retry loop
        Thread.sleep(1000);

//        System.out.print(page.asXml());

        final DomElement result = page.getElementById("result");
        assertNotNull(result);
        assertTrue(result.getTextContent().contains("pass"));
    }

    @Test
    public void runPageEditorTest() throws Exception {
        setUp("pageeditor-test.html");

        // TODO: replace by retry loop
        Thread.sleep(1000);

        // System.out.print(page.asXml());

        final DomElement result = page.getElementById("result");
        assertNotNull(result);

        assertTrue(result.getTextContent().contains("pass"));

        final DomElement instance = page.getElementById("Hippo.ChannelManager.TemplateComposer.Instance");
        assertNotNull(instance);
    }

}
