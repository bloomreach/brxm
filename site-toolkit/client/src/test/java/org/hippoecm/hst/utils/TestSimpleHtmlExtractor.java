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
package org.hippoecm.hst.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestSimpleHtmlExtractor {

    private static final String SIMPLE_HTML =
        "<html>\n" +
        "<head>\n" +
        "<title>Hello</title>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>Hello, World!</h1>\n" +
        "</body>\n" +
        "</html>";
    
    @Test
    public void testHtmlExtractor() throws Exception {
        String titleInnerContent = SimpleHtmlExtractor.getTagInnerContent(SIMPLE_HTML, "title");
        assertEquals("title content is not properly extracted: " + titleInnerContent.trim(), "Hello", titleInnerContent.trim());
        
        String bodyInnerContent = SimpleHtmlExtractor.getTagInnerContent(SIMPLE_HTML, "body");
        assertEquals("body content is not properly extracted: " + bodyInnerContent.trim(), "<h1>Hello, World!</h1>", bodyInnerContent.trim());
    }
    
}