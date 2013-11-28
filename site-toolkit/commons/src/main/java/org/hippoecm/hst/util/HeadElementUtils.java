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
package org.hippoecm.hst.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.core.component.HeadElement;

/**
 * HeadElementUtils
 * 
 * @version $Id$
 */
public class HeadElementUtils {

    private static final int HEX = 16;

    private static final String[] KNOWN_ENTITIES = { "gt", "amp", "lt", "apos", "quot" };

    private static final Set<String> EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET = new HashSet<String>(Arrays
            .asList(new String[] { "SCRIPT", "STYLE", "TITLE" }));

    private static final Set<String> PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET = new HashSet<String>(Arrays
            .asList(new String[] { "SCRIPT", "STYLE" }));

    private HeadElementUtils() {
    }
    
    public static String toHtmlString(final HeadElement headElement) {
        String tagName = headElement.getTagName().toUpperCase();
        boolean isExpanedEmptyElements = EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        boolean isPreformattedTextContent = PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, false);
    }

    public static String toXhtmlString(final HeadElement headElement) {
        return toXhtmlString(headElement, false);
    }
    
    public static String toXhtmlString(final HeadElement headElement, boolean commentedOutCDATAMarker) {
        String tagName = headElement.getTagName().toUpperCase();
        boolean isExpanedEmptyElements = EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        boolean isPreformattedTextContent = PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, true, commentedOutCDATAMarker);
    }

    public static String toString(final HeadElement headElement, boolean isExpanedEmptyElements,
            boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA) {
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, isPreformattedTextContentInCDATA, false);
    }
    
    public static String toString(final HeadElement headElement, boolean isExpanedEmptyElements,
            boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA, boolean commentedOutCDATAMarker) {
        StringWriter writer = new StringWriter(80);

        try {
            writeHeadElement(writer, headElement, isExpanedEmptyElements, isPreformattedTextContent,
                    isPreformattedTextContentInCDATA, commentedOutCDATAMarker);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return writer.toString();
    }

    public static void writeHeadElement(final Writer writer, final HeadElement headElement,
            boolean isExpandEmptyElements, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA)
            throws IOException {
        writeHeadElement(writer, headElement, isExpandEmptyElements, isPreformattedTextContent, isPreformattedTextContentInCDATA, false);
    }

    public static void writeHeadElement(final Writer writer, final HeadElement headElement,
            boolean isExpandEmptyElements, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA,
            boolean commentedOutCDATAMarker) throws IOException {
        String tagName = headElement.getTagName();
        String capitalizedTagName = tagName.toUpperCase();

        if (!"#TEXT".equals(capitalizedTagName)) {
            writer.write('<');
            writer.write(tagName);
        }

        for (Map.Entry<String, String> entry : headElement.getAttributeMap().entrySet()) {
            writer.write(' ');
            writer.write(entry.getKey());
            writer.write("=\"");
            writer.write(XmlUtils.encode(entry.getValue()));
            writer.write("\"");
        }

        if (!headElement.hasChildHeadElements()) {
            String textContent = headElement.getTextContent();

            if (!isExpandEmptyElements && (textContent == null || "".equals(textContent))) {
                writer.write("/>");
            } else {

                if (!"#TEXT".equals(capitalizedTagName)) {
                    writer.write('>');
                }

                if (textContent != null) {
                    if (isPreformattedTextContent) {
                        if (isPreformattedTextContentInCDATA) {
                            if (!"".equals(textContent)) {
                                if (commentedOutCDATAMarker) {
                                    if ("SCRIPT".equals(capitalizedTagName)) {
                                        writer.write("\n//<![CDATA[\n");
                                        writer.write(textContent);
                                        writer.write("\n//]]>\n");
                                    } else if ("STYLE".equals(capitalizedTagName)) {
                                        writer.write("\n/*<![CDATA[*/\n");
                                        writer.write(textContent);
                                        writer.write("\n/*]]>*/\n");
                                    } else {
                                        writer.write("<![CDATA[");
                                        writer.write(textContent);
                                        writer.write("]]>");
                                    }
                                } else {
                                    writer.write("<![CDATA[");
                                    writer.write(textContent);
                                    writer.write("]]>");
                                }
                            }
                        } else {
                            writer.write(textContent);
                        }
                    } else {
                        writer.write(XmlUtils.encode(textContent));
                    }
                }

                if (!"#TEXT".equals(capitalizedTagName)) {
                    writer.write("</");
                    writer.write(tagName);
                    writer.write('>');
                }
            }
        } else {
            writer.write(">");

            for (HeadElement childHeadElement : headElement.getChildHeadElements()) {
                writeHeadElement(writer, childHeadElement, isPreformattedTextContent, isExpandEmptyElements,
                        isPreformattedTextContentInCDATA);
            }

            writer.write("</");
            writer.write(tagName);
            writer.write('>');
        }
    }

}
