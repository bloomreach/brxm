/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.xinha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class HtmlProcessorTest {

    @Test
    public void testPrefixInternalImage() {
        String text = "testing 1 2 3 <img src=\"link\"/>";
        String processed = XinhaHtmlProcessor.prefixImageLinks(text, "test-prefix/");
        assertEquals("testing 1 2 3 <img src=\"test-prefix/link\"/>", processed);
    }

    @Test
    public void testPrefixExternalImage() {
        String text = "testing 1 2 3 <img src=\"http://link\"/>";
        String processed = XinhaHtmlProcessor.prefixImageLinks(text, "test-prefix/");
        assertEquals("testing 1 2 3 <img src=\"http://link\"/>", processed);
    }

    @Test
    public void testGetInternalLinks() {
        String text = "testing 1 2 3 <a href=\"link-1\">link 1</a>\n"+
            "more text <a href=\"http://test\">test</a>\n"+
            "and an image <img src=\"link-2/subnode\"/>";
        Set<String> links = XinhaHtmlProcessor.getInternalLinks(text);
        assertEquals(2, links.size());
        assertTrue(links.contains("link-1"));
        assertTrue(links.contains("link-2"));
    }

    @Test
    public void testMultilineGetInternalLinks() {
        String text="testing 1 2 3 <a\nhref=\"link\">link</a>";
        Set<String> links = XinhaHtmlProcessor.getInternalLinks(text);
        assertEquals(1, links.size());
        assertTrue(links.contains("link"));
    }

}
