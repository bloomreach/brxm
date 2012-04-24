/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.validator;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.CharMatcher;

import org.apache.wicket.IClusterable;
import org.cyberneko.html.parsers.SAXParser;
import org.hippoecm.frontend.validation.ValidationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HtmlValidator implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final String INVALID_XML = "invalid-xml";
    public static final String HTML_IS_EMPTY = "html-is-empty";

    public static final String[] VALID_ELEMENTS = new String[] {"img", "object", "embed", "form", "applet"};

    static class Handler extends DefaultHandler {
        boolean valid = false;

        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            String value = new String(chars, start, length).intern();
            if(CharMatcher.INVISIBLE.negate().matchesAnyOf(value)) {
                valid = true;
            }
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            for(String element : VALID_ELEMENTS) {
                if(element.equalsIgnoreCase(localName)) {
                    valid = true;
                    break;
                }
            }
        }

        boolean isValid() {
            return valid;
        }
    }

    public Set<String> validateNonEmpty(String html) throws ValidationException {
        Set<String> result = new HashSet<String>();
        Handler handler = new Handler();
        try {
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(html));

            SAXParser parser = new SAXParser();
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://cyberneko.org/html/features/override-namespaces", false);
            parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", false);
            parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");
            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");

            parser.setContentHandler(handler);
            parser.parse(is);

            if (!handler.isValid()) {
                result.add(HTML_IS_EMPTY);
            }
        } catch (SAXException e) {
            result.add(INVALID_XML);
        } catch (IOException e) {
            throw new ValidationException("Input/output error", e);
        }
        return result;
    }
}
