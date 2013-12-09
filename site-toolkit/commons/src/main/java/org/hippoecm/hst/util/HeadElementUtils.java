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
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    
    public static String toHtmlString(final Element headElement) {
        String tagName = headElement.getTagName().toUpperCase();
        boolean isExpanedEmptyElements = EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        boolean isPreformattedTextContent = PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, false);
    }

    public static String toXhtmlString(final Element headElement) {
        return toXhtmlString(headElement, false);
    }
    
    public static String toXhtmlString(final Element headElement, boolean commentedOutCDATAMarker) {
        String tagName = headElement.getTagName().toUpperCase();
        boolean isExpanedEmptyElements = EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        boolean isPreformattedTextContent = PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, true, commentedOutCDATAMarker);
    }

    public static String toString(final Element headElement, boolean isExpanedEmptyElements,
            boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA) {
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, isPreformattedTextContentInCDATA, false);
    }
    
    public static String toString(final Element headElement, boolean isExpanedEmptyElements,
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

    public static void writeHeadElement(final Writer writer, final Node headElement,
            boolean isExpandEmptyElements, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA)
            throws IOException {
        writeHeadElement(writer, headElement, isExpandEmptyElements, isPreformattedTextContent, isPreformattedTextContentInCDATA, false);
    }

    public static void writeHeadElement(final Writer writer, final Node headElement,
            boolean isExpandEmptyElements, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA,
            boolean commentedOutCDATAMarker) throws IOException {

        String tagName = headElement.getNodeName();
        String capitalizedTagName = tagName.toUpperCase();
        String textContent = headElement.getTextContent();

        if (!writeStartTag(writer, headElement, isExpandEmptyElements)) return;

        if (textContent != null && !"".equals(textContent)) {
            if (writeTextContent(writer, headElement, isPreformattedTextContent, isPreformattedTextContentInCDATA, commentedOutCDATAMarker)) {
                return;
            }
        }

        NodeList childNodes = headElement.getChildNodes();
        for (int i=0; i<childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            writeHeadElement(writer, childNode,
                    isExpandEmptyElements,
                    isPreformattedTextContent,
                    isPreformattedTextContentInCDATA,
                    commentedOutCDATAMarker);
        }

        writeEndTag(writer, tagName, capitalizedTagName);
    }

    private static boolean writeStartTag(Writer writer, Node headElement, boolean isExpandEmptyElements) throws IOException {
        String tagName = headElement.getNodeName();
        String capitalizedTagName = tagName.toUpperCase();
        String textContent = headElement.getTextContent();

        if (!"#CDATA-SECTION".equals(capitalizedTagName) && !"#TEXT".equals(capitalizedTagName)) {
            writer.write('<');
            writer.write(tagName);
        }

        if (headElement.hasAttributes()) {
            NamedNodeMap attributes = headElement.getAttributes();
            for (int i=0; i<attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                writer.write(' ');
                writer.write(attribute.getNodeName());
                writer.write("=\"");
                writer.write(XmlUtils.encode(attribute.getNodeValue()));
                writer.write("\"");
            }
        }

        if (!isExpandEmptyElements && !headElement.hasChildNodes() && (textContent == null || "".equals(textContent))) {
            writer.write("/>");
            return false;
        } else if (!"#CDATA-SECTION".equals(capitalizedTagName) && !"#TEXT".equals(capitalizedTagName)) {
            writer.write('>');
        }
        return true;
    }

    private static void writeEndTag(Writer writer, String tagName, String capitalizedTagName) throws IOException {
        if (!"#CDATA-SECTION".equals(capitalizedTagName) && !"#TEXT".equals(capitalizedTagName)) {
            writer.write("</");
            writer.write(tagName);
            writer.write('>');
        }
    }

    private static boolean writeTextContent(Writer writer, Node headElement, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA, boolean commentedOutCDATAMarker) throws IOException {
        String tagName = headElement.getNodeName();
        String capitalizedTagName = tagName.toUpperCase();
        String textContent = headElement.getTextContent();

        if (commentedOutCDATAMarker) {
            if ("SCRIPT".equals(capitalizedTagName)) {
                writer.write("//<![CDATA[");
                writer.write(textContent);
                writer.write("//]]>");
            } else if ("STYLE".equals(capitalizedTagName)) {
                writer.write("/*<![CDATA[*/");
                writer.write(textContent);
                writer.write("/*]]>*/");
            } else {
                writer.write("<![CDATA[");
                writer.write(textContent);
                writer.write("]]>");
            }
            writeEndTag(writer, tagName, capitalizedTagName);
            return true;
        } else if (!headElement.hasChildNodes()) {
            if (isPreformattedTextContent) {
                if (isPreformattedTextContentInCDATA && !"".equals(textContent)) {
                    writer.write("<![CDATA[");
                    writer.write(textContent);
                    writer.write("]]>");
                } else {
                    writer.write(textContent);
                }
            } else {
                writer.write(XmlUtils.encode(textContent));
            }
            return true;
        }
        return false;
    }

}
