/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext;

import java.util.Set;

import org.apache.wicket.mock.MockHomePage;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RichTextProcessorTest {

    WicketTester tester;

    @Before
    public void createTester() {
        tester = new WicketTester();
        tester.startPage(MockHomePage.class);
    }

    @Test
    public void testPrefixInternalImage() {
        String text = "testing 1 2 3 <img src=\"link\"/>";
        String processed = RichTextProcessor.prefixInternalImageLinks(text, new IImageURLProvider() {
            public String getURL(String name) {
                return "test-prefix/" + name;
            }
        });
        assertEquals("testing 1 2 3 <img src=\"test-prefix/link\" facetselect=\"link\"/>", processed);
    }

    @Test
    public void testPrefixInternalImageWithResourceSuffix() {
        String text = "testing 1 2 3 <img src=\"link/{_document}/hippogallery:original\"/>";
        String processed = RichTextProcessor.prefixInternalImageLinks(text, new IImageURLProvider() {
            public String getURL(String name) {
                return "test-prefix/" + name;
            }
        });
        assertEquals("testing 1 2 3 <img src=\"test-prefix/link/{_document}/hippogallery:original\" facetselect=\"link/{_document}/hippogallery:original\" type=\"hippogallery:original\"/>", processed);
    }

    @Test
    public void testPrefixExternalImage() {
        String text = "testing 1 2 3 <img src=\"http://link\"/>";
        String processed = RichTextProcessor.prefixInternalImageLinks(text, new IImageURLProvider() {
            public String getURL(String name) {
                return "test-prefix/" + name;
            }
        });
        assertEquals("testing 1 2 3 <img src=\"http://link\"/>", processed);
    }

    @Test
    public void testGetInternalLinks() {
        String text = "testing 1 2 3 <a uuid=\"1234\">link 1</a>\n"+
            "more text <a href=\"http://test\">test</a>\n"+
            "and an image <img src=\"link-2/subnode\" uuid=\"5678\"/>";
        Set<String> links = RichTextProcessor.getInternalLinkUuids(text);
        assertEquals(2, links.size());
        assertTrue(links.contains("1234"));
        assertTrue(links.contains("5678"));
    }

    @Test
    public void testMultilineGetInternalLinks() throws Exception {
        String text="testing 1 2 3 <a\nuuid=\"1234\">link</a>";
        Set<String> links = RichTextProcessor.getInternalLinkUuids(text);
        assertEquals(1, links.size());
        assertTrue(links.contains("1234"));
    }

    @Test
    public void testFacetRestore() {
        String text="<img src=\"horriblyterriblelinkencoding\" facetselect=\"facet\" />";
        String restored = RichTextProcessor.restoreFacets(text);
        assertEquals("<img src=\"facet\"/>", restored);
    }

}
