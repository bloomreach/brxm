/*
 *  Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.easymock.EasyMock;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertThat;

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
    public void testCleanMultipleNonWhiteListedAttributes() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        String html = htmlCleanerPlugin.clean("<p foo=\"bar\" hippo=\"ok\">&nbsp;</p>");
        log.debug(html);
        // attribute 'foo' nor 'hippo' of p is on whitelist
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
        assertEquals("simple text <p>&nbsp;</p>", html);
    }

    @Test
    public void testCleanJavascriptInAttributes() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        // src attribute contains javascript
        String html = htmlCleanerPlugin.clean("<img src=\"jAvAsCrIpT:alert()\"");
        log.debug(html);
        assertEquals("<img src=\"\" />", html);
    }

    @Test
    public void testCleanEncodedJavascriptInAttributes() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        // src attribute contains encoded javascript
        String html = htmlCleanerPlugin.clean("<a href=\"&#106;&#97;&#118;&#97;&#115;&#99;&#114;&#105;&#112;&#116;" +
                "&#58;&#97;&#108;&#101;&#114;&#116;&#40;&#39;&#88;&#83;&#83;&#39;&#41;\">link</a>");
        log.debug(html);
        assertEquals("<a href=\"\">link</a>", html);
    }

    @Test
    public void testCharacterEntityConversion() throws Exception {
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, getPluginConfig());

        String html = htmlCleanerPlugin.clean("&nbsp; &gt; &lt; &amp; á &aacute;");
        log.debug(html);
        assertEquals("&nbsp; &gt; &lt; &amp; á á", html);
    }

    @Test
    public void testQuoteConversion() throws Exception {
        final IPluginConfig pluginConfig = getPluginConfig();
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        final String html = htmlCleanerPlugin.clean("' \" &apos; &quot;", false, null, null);

        assertEquals("' \" ' \"", html);
    }

    @Test
    public void expectScriptTagIsNotRemoved() throws Exception {
        final IPluginConfig pluginConfig = getPluginConfig();
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        final String original = "<h1><style type=\"text/css\" scoped>h1 {color:black;}</style>42</h1>";
        final String expected = "<h1><style type=\"text/css\" scoped=\"scoped\">h1 {color:black;}</style>42</h1>";
        final String html = htmlCleanerPlugin.clean(original, false, null, null);

        assertEquals(expected, html);
    }

    @Test
    public void expectLoopFinishes() throws Exception {
        final IPluginConfig pluginConfig = getPluginConfig();
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        final String original = "<div>\n" +
                " <picture>\n" +
                "<source media=\"(max-width: 700px)\" sizes=\"(max-width: 500px) 50vw, 10vw\"\n" +
                "srcset=\"stick-figure-narrow.png 138w, stick-figure-hd-narrow.png 138w\">\n" +
                "\n" +
                "<source media=\"(max-width: 1400px)\" sizes=\"(max-width: 1000px) 100vw, 50vw\"\n" +
                "srcset=\"stick-figure.png 416w, stick-figure-hd.png 416w\">\n" +
                "\n" +
                "<img src=\"stick-original.png\" alt=\"Human\">\n" +
                "</picture>\n" +
                "</div>";
        final String expected = "<div>\n" +
                " <picture>\n" +
                "<audio><source media=\"(max-width: 700px)\" sizes=\"(max-width: 500px) 50vw, 10vw\" srcset=\"stick-figure-narrow.png 138w, stick-figure-hd-narrow.png 138w\" />\n" +
                "\n" +
                "<source media=\"(max-width: 1400px)\" sizes=\"(max-width: 1000px) 100vw, 50vw\" srcset=\"stick-figure.png 416w, stick-figure-hd.png 416w\" />\n" +
                "\n" +
                "<img src=\"stick-original.png\" alt=\"Human\" />\n" +
                "</audio></picture></div>";

        final String html = htmlCleanerPlugin.clean(original, false, null, null);

        assertEquals(expected, html);
    }

    @Test
    public void serviceIdIsConfigurable() {
        final IPluginContext context = EasyMock.createMock(IPluginContext.class);
        context.registerService(isA(HtmlCleanerPlugin.class), eq("myHtmlCleaner"));
        EasyMock.expectLastCall();
        replay(context);

        final IPluginConfig config = new JavaPluginConfig();
        config.put("service.id", "myHtmlCleaner");

        new HtmlCleanerPlugin(context, config);

        verify(context);
    }

    @Test
    public void testFilteringByAbsentOption() throws Exception {
        IPluginConfig pluginConfig = getPluginConfig();
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        String html = htmlCleanerPlugin.clean("<script>alert(\"xss\")</script>");
        assertEquals("", html);
    }

    @Test
    public void testFilteringBySetOption() throws Exception {
        IPluginConfig pluginConfig = getPluginConfig();
        pluginConfig.put("filter", true);
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        String html = htmlCleanerPlugin.clean("<script>alert(\"xss\")</script>");
        assertEquals("", html);
    }

    @Test
    public void testNoFilteringBySetOption() throws Exception {
        IPluginConfig pluginConfig = getPluginConfig();
        pluginConfig.put("filter", false);
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        String html = htmlCleanerPlugin.clean("<script>alert(\"xss\")</script>");
        assertEquals("<script>alert(\"xss\")</script>", html);
    }

    @Test
    public void testNoFilteringByCall() throws Exception {
        IPluginConfig pluginConfig = getPluginConfig();
        pluginConfig.put("filter", true);
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        String html = htmlCleanerPlugin.clean("<script>alert(\"xss\")</script>", false, null, null);
        assertEquals("<script>alert(\"xss\")</script>", html);
    }

    @Test
    public void testTraversingBeforeFiltering() throws Exception {
        IPluginConfig pluginConfig = getPluginConfig();
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        TagNameCollector collector = new TagNameCollector();
        String html = htmlCleanerPlugin.clean("<h1>Heading</h1><script>alert(\"xss\")</script>", true, collector, null);
        assertEquals("<h1>Heading</h1>", html);
        List<String> tags = collector.getTags();
        assertEquals(2, tags.size());
        assertThat(tags, JUnitMatchers.hasItems("h1", "script"));
    }

    @Test
    public void testTraversingAfterFiltering() throws Exception {
        IPluginConfig pluginConfig = getPluginConfig();
        final HtmlCleanerPlugin htmlCleanerPlugin = new HtmlCleanerPlugin(null, pluginConfig);

        TagNameCollector collector = new TagNameCollector();
        String html = htmlCleanerPlugin.clean("<h1>Heading</h1><script>alert(\"xss\")</script>", true, null, collector);
        assertEquals("<h1>Heading</h1>", html);
        List<String> tags = collector.getTags();
        assertEquals(1, tags.size());
        assertThat(tags, JUnitMatchers.hasItems("h1"));
    }

    private static class TagNameCollector implements TagNodeVisitor {

        private List<String> tags = new ArrayList<>();

        @Override
        public boolean visit(TagNode parent, HtmlNode node) {
            if (node instanceof TagNode) {
                TagNode tagNode = (TagNode) node;
                String name = tagNode.getName();
                if (name != null) {
                    tags.add(name);
                }
            }
            return true;
        }

        public List<String> getTags() {
            return tags;
        }
    }
}
