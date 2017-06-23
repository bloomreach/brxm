/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.visit;

import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.Tag;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HtmlTagTest {

    @Test
    public void testCreateTagFromTagNode() throws Exception {
        final TagNode tagNode = createMock(TagNode.class);
        assertNotNull(HtmlTag.from(tagNode));
    }

    @Test
    public void testCreateTagFromHtmlNodeIfInstanceOfTagNode() throws Exception {
        final HtmlNode htmlNode = createMock(HtmlNode.class);
        assertNull(HtmlTag.from(htmlNode));

        final TagNode tagNode = createMock(TagNode.class);
        assertNotNull(HtmlTag.from(tagNode));
    }

    @Test
    public void testTagName() throws Exception {
        assertEquals("img", HtmlTag.from("img").getName());
        assertEquals("img", HtmlTag.from("IMG").getName());
        assertEquals("img", HtmlTag.from("ImG").getName());
    }

    @Test
    public void testAddGetAttribute() throws Exception {
        final Tag tag = HtmlTag.from("img");
        assertNull(tag.getAttribute("attr"));

        tag.addAttribute("attr", "value");
        assertEquals("value", tag.getAttribute("attr"));
    }

    @Test
    public void testChangeAttribute() throws Exception {
        final Tag tag = HtmlTag.from("img");
        tag.addAttribute("attr", "value");
        tag.addAttribute("attr", "newValue");
        assertEquals("newValue", tag.getAttribute("attr"));
    }

    @Test
    public void testRemoveAttribute() throws Exception {
        final Tag tag = HtmlTag.from("img");
        // lenient
        tag.removeAttribute("attr");

        tag.addAttribute("attr", "value");
        tag.removeAttribute("attr");
        assertFalse(tag.hasAttribute("attr"));
    }

    @Test
    public void testTagHasAttribute() throws Exception {
        final Tag tag = HtmlTag.from("img");
        assertFalse(tag.hasAttribute("attr"));

        tag.addAttribute("attr", "value");
        assertTrue(tag.hasAttribute("attr"));

        tag.removeAttribute("attr");
        assertFalse(tag.hasAttribute("attr"));
    }
}
