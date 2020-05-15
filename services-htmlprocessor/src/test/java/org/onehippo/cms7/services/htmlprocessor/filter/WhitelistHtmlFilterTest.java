/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WhitelistHtmlFilterTest {

    private HtmlCleaner parser;
    private HtmlFilter filter;

    @Before
    public void setUp() {
        final CleanerProperties properties = new CleanerProperties();
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitXmlDeclaration(true);
        parser = new HtmlCleaner(properties);
        filter = new WhitelistHtmlFilter();
    }

    @Test
    public void testEmptyWhitelist() {
        final TagNode result = filterHtml("<div></div>");
        assertNull(result.findElementByName("div", true));
    }

    @Test
    public void testRootNotWhitelisted() {
        addToWhitelist("span");
        final TagNode result = filterHtml("<div><span></span></div>");
        assertNull(result.findElementByName("div", true));
        assertNull(result.findElementByName("span", true));
    }

    @Test
    public void testRootIsWhitelisted() {
        addToWhitelist("div");
        final TagNode result = filterHtml("<div>&nbsp;</div>");

        final TagNode div = result.findElementByName("div", false);
        assertNotNull(div);
    }

    @Test
    public void testWhitelistedChildNodes() {
        addToWhitelist("p", "em", "strong");
        final TagNode result = filterHtml("<p><em></em><strong></strong></p>");

        final TagNode p = result.findElementByName("p", true);
        assertNotNull(p);
        assertTrue(p.hasChildren());
        assertEquals("em", p.getChildTagList().get(0).getName());
        assertEquals("strong", p.getChildTagList().get(1).getName());
    }

    @Test
    public void testCleanNonWhiteListedTag() throws Exception {
        TagNode result = filterHtml("<script>alert(\"xss\")</script>");
        // script element is not on whitelist
        assertNull(result.findElementByName("script", true));

        result = filterHtml("<ScRiPT>alert(\"xss\")</sCrIpT>");
        assertNull(result.findElementByName("script", true));
    }

    @Test
    public void testDescendantsWhitelisted() {
        addToWhitelist("div", "p", "em");
        final TagNode result = filterHtml("<div><p><em>text</em><em>test2</em></p><ul><li>list</li></ul></div><span>&nbsp;</span>");

        final TagNode div = result.findElementByName("div", true);
        assertNotNull(div);
        assertEquals(1, div.getChildTags().length);

        final TagNode p = div.findElementByName("p", false);
        assertNotNull(p);
        assertEquals(2, p.getChildTags().length);

        final List<? extends TagNode> ems = p.getElementListByName("em", false);
        assertEquals(2, ems.size());

        // span should be cleaned
        assertNull(result.findElementByName("span", true));
    }

    @Test
    public void testPlainText() throws Exception {
        final TagNode result = filterHtml("simple text");
        assertEquals("simple text", result.getText().toString());
    }

    @Test
    public void testTextInElement() throws Exception {
        addToWhitelist("p");

        final TagNode result = filterHtml("simple text <p>&nbsp;</p>");
        assertEquals("simple text &nbsp;", result.getText().toString());

        final TagNode p = result.getChildTags()[0];
        assertEquals("&nbsp;", p.getText().toString());
    }

    @Test
    public void testAttributesWhitelisted() throws Exception {
        addToWhitelist(Element.create("img", "src"), Element.create("div", "id", "class"));

        final TagNode result = filterHtml("<img src=\"img.gif\" class=\"img-class\"/>" +
                                    "<div id=\"div-id\" class=\"div-class\" alt=\"div-alt\"></div>");

        final TagNode img = result.findElementByName("img", true);
        assertEquals("img.gif", img.getAttributeByName("src"));
        assertFalse(img.hasAttribute("class"));

        final TagNode div = result.findElementByName("div", true);
        assertEquals("div-id", div.getAttributeByName("id"));
        assertEquals("div-class", div.getAttributeByName("class"));
        assertFalse(div.hasAttribute("alt"));
    }

    @Test
    public void testCleanNonWhiteListedAttributes() throws Exception {
        addToWhitelist("p");
        TagNode result = filterHtml("<p foo=\"bar\">&nbsp;</p>");

        // attribute foo of p is not on whitelist
        assertNull(result.findElementHavingAttribute("foo", true));

        result = filterHtml("<p foo=\"bar\" hippo=\"ok\">&nbsp;</p>");
        assertNull(result.findElementHavingAttribute("foo", true));
        assertNull(result.findElementHavingAttribute("hippo", true));
    }

    @Test
    public void testCleanJavascriptInAttributes() throws Exception {
        addToWhitelist(Element.create("img", "src"));
        final TagNode result = filterHtml("<img src=\"jAvAsCrIpT:alert()\"");

        // src attribute contains javascript
        final TagNode img = result.findElementByName("img", true);
        assertNotNull(img);
        assertEquals("", img.getAttributeByName("src"));
    }

    // Verify fix for CMS-7701 - See comment https://issues.onehippo.com/browse/CMS-7701?focusedCommentId=274200&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-274200
    @Test
    public void testCleanEncodedJavascriptInAttributes() throws Exception {
        addToWhitelist(Element.create("a", "href"));
        // href attribute contains encoded javascript
        final TagNode result = filterHtml("<a href=\"&#106;&#97;&#118;&#97;&#115;&#99;&#114;&#105;&#112;&#116;" +
                                            "&#58;&#97;&#108;&#101;&#114;&#116;&#40;&#39;&#88;&#83;&#83;&#39;&#41;\">link</a>");

        final TagNode img = result.findElementByName("a", true);
        assertNotNull(img);
        assertEquals("", img.getAttributeByName("href"));
    }

    @Test
    public void testCleanJavascriptProtocolArgumentTrue() throws Exception {
        filter = new WhitelistHtmlFilter(new ArrayList<>(), true);
        addToWhitelist(Element.create("a", "href", "onclick"));

        // src attribute contains javascript:
        TagNode result = filterHtml("<a href=\"#\" onclick=\"javascript:lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>");
        TagNode a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("", a.getAttributeByName("onclick"));

        // src attribute contains javascript: + space
        result = filterHtml("<a href=\"#\" onclick=\"javascript: lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>");
        a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("", a.getAttributeByName("onclick"));
    }

    @Test
    public void testCleanJavascriptProtocolArgumentFalse() throws Exception {
        filter = new WhitelistHtmlFilter(new ArrayList<>(), false);
        addToWhitelist(Element.create("a", "href", "onclick"));
        final TagNode result = filterHtml("<a href=\"#\" onclick=\"javascript:lancerPu('XXXcodepuXXX')\">XXXTexteXXX</a>");

        // src attribute contains javascript:
        final TagNode a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("javascript:lancerPu('XXXcodepuXXX')", a.getAttributeByName("onclick"));
    }

    @Test
    public void testCleanJavascriptProtocolNewLine() throws Exception {
        filter = new WhitelistHtmlFilter(new ArrayList<>(), true);
        addToWhitelist(Element.create("a", "href"));

        // check new lines
        TagNode result = filterHtml("<a href=\"jav&#x0A;ascript:alert('XSS');\">test</a>");
        TagNode a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("", a.getAttributeByName("href"));

        result = filterHtml("<a href=\"javascript\n:alert('XSS');\">test</a>");
        a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("javascript :alert('XSS');", a.getAttributeByName("href"));
    }

    @Test
    public void testCleanDataProtocol() throws Exception {
        filter = new WhitelistHtmlFilter(new ArrayList<>(), true);
        addToWhitelist(Element.create("a", "href"));

        // href attribute contains data:
        TagNode result = filterHtml("<a href=\"data:testData\">data</a>");
        TagNode a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("", a.getAttributeByName("href"));

        // href attribute contains data: + space
        result = filterHtml("<a href=\"data: testData\">data</a>");
        a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("", a.getAttributeByName("href"));
    }

    @Test
    public void testDataPrefixOfFileNameIsNotCleaned() throws Exception {
        filter = new WhitelistHtmlFilter(new ArrayList<>(), true);
        addToWhitelist(Element.create("a", "href"));

        // href attribute start with 'data' but is not a data protocol
        TagNode result = filterHtml("<a href=\"data-science.pdf\">data science</a>");
        TagNode a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("data-science.pdf", a.getAttributeByName("href"));
    }

    @Test
    public void testCleanDataProtocolNewLine() throws Exception {
        filter = new WhitelistHtmlFilter(new ArrayList<>(), true);
        addToWhitelist(Element.create("a", "href"));
        // check new lines
        TagNode result = filterHtml("<a href=\"data\n:testData\">data</a>");
        TagNode a = result.findElementByName("a", true);
        assertNotNull(a);
        assertEquals("data :testData", a.getAttributeByName("href"));
    }

    private TagNode filterHtml(final String html) {
        return filter.apply(parser.clean(html));
    }

    private void addToWhitelist(final String... tags) {
        Arrays.stream(tags).forEach(tag -> filter.add(Element.create(tag)));
    }

    private void addToWhitelist(final Element... elements) {
        Arrays.stream(elements).forEach(element -> filter.add(element));
    }

}
