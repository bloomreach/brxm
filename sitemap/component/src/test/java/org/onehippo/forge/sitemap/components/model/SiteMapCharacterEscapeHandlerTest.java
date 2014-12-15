/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.model;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static junit.framework.Assert.assertEquals;

public class SiteMapCharacterEscapeHandlerTest {

    @Test
    public void testEscape() throws Exception {
        final CaptureContentHandler captureHandler = new CaptureContentHandler();
        final SiteMapCharacterEscapeHandler escapeHandler = new SiteMapCharacterEscapeHandler(captureHandler);
        String s = "foo \" bar \' baz & quux";
        escapeHandler.characters(s.toCharArray(), 0, s.length());
        assertEquals("foo &quot; bar &apos; baz &amp; quux", captureHandler.characters);
        escapeHandler.characters(s.toCharArray(), 6, 9);
        assertEquals("bar &apos; baz", captureHandler.characters);
    }

    private static class CaptureContentHandler extends DefaultHandler {
        private String characters;
        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            this.characters = new String(ch, start, length);
        }
    }
}