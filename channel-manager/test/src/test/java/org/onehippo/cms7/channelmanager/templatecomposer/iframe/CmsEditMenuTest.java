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
package org.onehippo.cms7.channelmanager.templatecomposer.iframe;

import java.util.List;
import java.util.NoSuchElementException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CmsEditMenuTest extends AbstractTemplateComposerTest {

    @Before
    public void startPage() throws Exception {
        setUp("cmseditmenu.html");
        initializeIFrameHead();
        initializeTemplateComposer(false, false);
    }

    @Test
    public void buttonReplacesHtmlComment() throws Exception {
        final HtmlElement editMenuLink = getEditMenuLink();
        assertTrue("The 'hst:cmseditmenu' tag should replace the HTML comment", isMetaDataConsumed(getEditMenuLink()));
        assertFalse("There should be no 'exception' events'", isPublished(iframeToHostMessages, "exception"));
    }

    private HtmlElement getEditMenuLink() {
        final List<HtmlElement> divs = page.getElementsByTagName("a");
        for (HtmlElement div : divs) {
            if (eval("HST.CLASS.EDITMENU").equals(div.getAttribute("class"))) {
                return div;
            }
        }
        return null;
    }

    @Test
    public void clickButtonSendsMessage() throws Exception {
        HtmlElement editMenuLink = getEditMenuLink();
        editMenuLink.click();

        page.getWebClient().waitForBackgroundJavaScript(100);

        assertTrue("Clicking the 'edit menu' button should fire the 'edit-menu' event", isPublished(iframeToHostMessages, "edit-menu"));
    }

}
