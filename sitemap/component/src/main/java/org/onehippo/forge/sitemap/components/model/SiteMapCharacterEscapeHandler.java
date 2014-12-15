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

import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.utilities.xml.ProxyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


public class SiteMapCharacterEscapeHandler extends ProxyContentHandler {

    private static final Map<Character, String> CHARS_TO_ESCAPE = new HashMap<Character, String>() {{
        put('&', "&amp;");
        put('\'', "&apos;");
        put('"', "&quot;");
        put('>', "&gt;");
        put('<', "&lt;");
    }};

    public SiteMapCharacterEscapeHandler(final ContentHandler handler) {
        super(handler);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        final StringBuilder builder = new StringBuilder(length+20);
        for (int i = start; i < length+start; i++) {
            final String escape = CHARS_TO_ESCAPE.get(ch[i]);
            builder.append(escape == null ? ch[i] : escape);
        }
        super.characters(builder.toString().toCharArray(), 0, builder.length());
    }

}