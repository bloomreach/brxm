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
public class HeadElementUtils
{

    private static final int HEX = 16;
    
    private static final String[] KNOWN_ENTITIES = {"gt", "amp", "lt", "apos", "quot"};
    
    private static final Set<String> EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET = 
        new HashSet<String>(Arrays.asList(new String [] { "SCRIPT", "STYLE", "TITLE" } ));
    
    private static final Set<String> PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET = 
        new HashSet<String>(Arrays.asList(new String [] { "SCRIPT", "STYLE" } ));
    
    private HeadElementUtils()
    {
    }
    
    public static String toHtmlString(final HeadElement headElement)
    {
        String tagName = headElement.getTagName().toUpperCase();
        boolean isExpanedEmptyElements = EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        boolean isPreformattedTextContent = PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, false);
    }
    
    public static String toXhtmlString(final HeadElement headElement)
    {
        String tagName = headElement.getTagName().toUpperCase();
        boolean isExpanedEmptyElements = EXPANDABLE_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        boolean isPreformattedTextContent = PREFORMATTED_HEAD_ELEMENT_TAG_NAME_SET.contains(tagName);
        return toString(headElement, isExpanedEmptyElements, isPreformattedTextContent, true);
    }
    
    public static String toString(final HeadElement headElement, boolean isExpanedEmptyElements, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA)
    {
        StringWriter writer = new StringWriter(80);
        
        try
        {
            writeHeadElement(writer, headElement, isExpanedEmptyElements, isPreformattedTextContent, isPreformattedTextContentInCDATA);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        
        return writer.toString();
    }
    
    public static void writeHeadElement(final Writer writer, final HeadElement headElement, boolean isExpandEmptyElements, boolean isPreformattedTextContent, boolean isPreformattedTextContentInCDATA) throws IOException
    {
        String tagName = headElement.getTagName();
        writer.write('<');
        writer.write(tagName);
        
        for (Map.Entry<String, String> entry : headElement.getAttributeMap().entrySet())
        {
            writer.write(' ');
            writer.write(entry.getKey());
            writer.write("=\"");
            writer.write(encode(entry.getValue()));
            writer.write("\"");
        }
        
        if (!headElement.hasChildHeadElements())
        {
            String textContent = headElement.getTextContent();
            
            if (!isExpandEmptyElements && (textContent == null || "".equals(textContent)))
            {
                writer.write("/>");
            }
            else
            {
                writer.write('>');
                
                if (textContent != null)
                {
                    if (isPreformattedTextContent)
                    {
                        if (isPreformattedTextContentInCDATA)
                        {
                            writer.write("<![CDATA[");
                            writer.write(textContent);
                            writer.write("]]>");
                        }
                        else
                        {
                            writer.write(textContent);
                        }
                    }
                    else
                    {
                        writer.write(encode(textContent));
                    }
                }
                
                writer.write("</");
                writer.write(tagName);
                writer.write('>');
            }
        }
        else
        {
            writer.write(">\n");
            
            for (HeadElement childHeadElement : headElement.getChildHeadElements())
            {
                writeHeadElement(writer, childHeadElement, isPreformattedTextContent, isExpandEmptyElements, isPreformattedTextContentInCDATA);
                writer.write('\n');
            }
            
            writer.write("</");
            writer.write(tagName);
            writer.write('>');
        }
    }
    
    /**
     * Escape &lt;, &gt; &amp; &apos;, &quot; as their entities and
     * drop characters that are illegal in XML documents.
     * @param value the string to encode.
     * @return the encoded string.
     */
    public static String encode(String value) {
        StringBuffer sb = new StringBuffer();
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            case '\"':
                sb.append("&quot;");
                break;
            case '&':
                int nextSemi = value.indexOf(";", i);
                if (nextSemi < 0
                    || !isReference(value.substring(i, nextSemi + 1))) {
                    sb.append("&amp;");
                } else {
                    sb.append('&');
                }
                break;
            default:
                if (isLegalCharacter(c)) {
                    sb.append(c);
                }
                break;
            }
        }
        return sb.substring(0);
    }
    
    /**
     * Is the given argument a character or entity reference?
     * @param ent the value to be checked.
     * @return true if it is an entity.
     */
    private static boolean isReference(String ent) {
        if (!(ent.charAt(0) == '&') || !ent.endsWith(";")) {
            return false;
        }

        if (ent.charAt(1) == '#') {
            if (ent.charAt(2) == 'x') {
                try {
                    // CheckStyle:MagicNumber OFF
                    Integer.parseInt(ent.substring(3, ent.length() - 1), HEX);
                    // CheckStyle:MagicNumber ON
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            } else {
                try {
                    Integer.parseInt(ent.substring(2, ent.length() - 1));
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }
        }

        String name = ent.substring(1, ent.length() - 1);
        for (int i = 0; i < KNOWN_ENTITIES.length; i++) {
            if (name.equals(KNOWN_ENTITIES[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Is the given character allowed inside an XML document?
     *
     * <p>See XML 1.0 2.2 <a
     * href="http://www.w3.org/TR/1998/REC-xml-19980210#charsets">
     * http://www.w3.org/TR/1998/REC-xml-19980210#charsets</a>.</p>
     * @param c the character to test.
     * @return true if the character is allowed.
     */
    private static boolean isLegalCharacter(char c) {
        // CheckStyle:MagicNumber OFF
        if (c == 0x9 || c == 0xA || c == 0xD) {
            return true;
        } else if (c < 0x20) {
            return false;
        } else if (c <= 0xD7FF) {
            return true;
        } else if (c < 0xE000) {
            return false;
        } else if (c <= 0xFFFD) {
            return true;
        }
        // CheckStyle:MagicNumber ON
        return false;
    }
    
}
