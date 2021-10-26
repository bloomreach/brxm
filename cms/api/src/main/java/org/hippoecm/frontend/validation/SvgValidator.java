/*
 * Copyright 2021 Bloomreach Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SvgValidator {

    /**
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/SVG/Element">
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element
     * </a>
     */
    static final Set<String> OFFENDING_SVG_ELEMENTS = Stream.of("script").collect(Collectors.toSet());
    /**
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute">
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute
     * </a>
     */

    static final Set<String> OFFENDING_SVG_ATTRIBUTES = Stream.of("onbegin", "onend", "onrepeat", "onabort", "onerror",
            "onresize", "onscroll", "onunload", "oncopy", "oncut", "onpaste", "oncancel", "oncanplay",
            "oncanplaythrough", "onchange", "onclick", "onclose", "oncuechange", "ondblclick", "ondrag", "ondragend",
            "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop", "ondurationchange", "onemptied",
            "onended", "onerror", "onfocus", "oninput", "oninvalid", "onkeydown", "onkeypress", "onkeyup", "onload",
            "onloadeddata", "onloadedmetadata", "onloadstart", "onmousedown", "onmouseenter", "onmouseleave",
            "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel", "onpause", "onplay", "onplaying",
            "onprogress", "onratechange", "onreset", "onresize", "onscroll", "onseeked", "onseeking", "onselect",
            "onshow", "onstalled", "onsubmit", "onsuspend", "ontimeupdate", "ontoggle", "onvolumechange", "onwaiting",
            "onactivate", "onfocusin", "onfocusout").collect(Collectors.toSet());

    private SvgValidator() {
    }


    public static SvgValidationResult validate(final InputStream is) throws
            ParserConfigurationException,
            SAXException,
            IOException {

        SvgValidationResult.SvgValidationResultBuilder builder = SvgValidationResult.builder();
        DefaultHandler handler = new DefaultHandler() {

            private boolean inStyleElement;

            @Override
            public void startElement(final String uri, final String localName, final String qName,
                                     final Attributes attributes) {
                if ("style".equals(qName)) {
                    inStyleElement = true;
                }
                if (OFFENDING_SVG_ELEMENTS.contains(qName)) {
                    builder.offendingElement(qName);
                }
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attributeName = attributes.getQName(i);
                    if (OFFENDING_SVG_ATTRIBUTES.contains(attributeName)) {
                        builder.offendingAttribute(attributeName);
                    }
                }
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) throws
                    SAXException {
                if (inStyleElement) {
                    inStyleElement = false;
                }
            }

            @Override
            public void characters(final char[] ch, final int start, final int length) throws
                    SAXException {
                if (inStyleElement && StringUtils.containsIgnoreCase(String.valueOf(ch), "javascript")) {
                    throw new SAXException("Javascript inside style element is not supported");
                }
            }
        };

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        SAXParser parser = factory.newSAXParser();
        parser.parse(is, handler);
        return builder.build();
    }
}
