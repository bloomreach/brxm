/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.htmlcleaner;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class HtmlCleanerPluginTest extends PluginTest {

    private IPluginConfig getPluginConfig() throws Exception {
        Node cleanerConfigNode = root.getNode("cleaner.config");
        JcrNodeModel nodeModel = new JcrNodeModel(cleanerConfigNode);
        return new JcrPluginConfig(nodeModel);
    }

    @Test
    public void testCleanNonWhiteListedTag() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        String html = htmlCleanerPlugin.clean("<script>alert(\"xss\")</script>");
        log.debug(html);
        // script element is not on whitelist
        assertEquals("", html);

        html = htmlCleanerPlugin.clean("<ScRiPT>alert(\"xss\")</sCrIpT>");
        log.debug(html);
        assertEquals("", html);
    }

    @Test
    public void testCleanNonWhiteListedAttribute() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        String html = htmlCleanerPlugin.clean("<p foo=\"bar\">&nbsp;</p>");
        log.debug(html);
        // attribute foo of p is not on whitelist
        assertEquals("<p>&nbsp;</p>", html);
    }

    @Test
    public void testCleanPlainText() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        String html = htmlCleanerPlugin.clean("simple text");
        log.debug(html);
        assertEquals("simple text", html);
    }

    @Test
    public void testCleanMultipleRootElements() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        String html = htmlCleanerPlugin.clean("simple text <p>&nbsp;</p>");
        log.debug(html);
        assertEquals("simple text \n<p>&nbsp;</p>", html);
    }

    @Test
    public void testClean() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        // src attribute contains javascript
        String html = htmlCleanerPlugin.clean("<img src=\"jAvAsCrIpT:alert()\"");
        log.debug(html);
        assertEquals("<img src=\"\" />", html);
    }

}
