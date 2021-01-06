/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.core.component.WrapperElement;

/**
 * WrapperElementUtils
 * 
 * @version $Id$
 */
public class WrapperElementUtils
{

    private WrapperElementUtils()
    {
    }
    
    public static String toString(final WrapperElement wrapperElement)
    {
        StringWriter writer = new StringWriter(80);
        
        try
        {
            writeWrapperElement(writer, wrapperElement, null, 0, 0);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        
        return writer.toString();
    }
    
    public static void writeWrapperElement(final Writer writer, WrapperElement wrapperElement, char [] contentBuffer, int off, int len) throws IOException
    {
        writeWrapperElementStart(writer, wrapperElement);
        
        String textContent = wrapperElement.getTextContent();
        
        if (textContent != null)
        {
            writer.write(XmlUtils.encode(textContent));
        }
        
        if (contentBuffer != null) {
            writer.write(contentBuffer, off, len);
        }
        
        writeWrapperElementEnd(writer, wrapperElement);
    }
    
    private static void writeWrapperElementStart(final Writer writer, WrapperElement wrapperElement) throws IOException {
        writer.write('<');
        writer.write(wrapperElement.getTagName());

        Set<String> skipEscapingAttrs = wrapperElement.getSkipEscapingAttrs();
        for (Map.Entry<String, String> entry : wrapperElement.getAttributeMap().entrySet())
        {
            writer.write(' ');
            writer.write(entry.getKey());
            writer.write("=\"");
            writer.write(skipEscapingAttrs.contains(entry.getKey()) ? entry.getValue() : XmlUtils.encode(entry.getValue()));
            writer.write("\"");
        }
        
        writer.write('>');
    }
    
    private static void writeWrapperElementEnd(final Writer writer, WrapperElement wrapperElement) throws IOException {
        writer.write("</");
        writer.write(wrapperElement.getTagName());
        writer.write('>');
    }
    
    private static void writeWrapperElementEnd(final OutputStream out, WrapperElement wrapperElement) throws IOException {
        out.write("</".getBytes());
        out.write(wrapperElement.getTagName().getBytes());
        out.write('>');
    }
    
    public static void writeWrapperElement(final OutputStream out, String encoding, WrapperElement wrapperElement, byte [] contentBuffer, int off, int len) throws IOException
    {
        OutputStreamWriter writer = null;
        
        if (encoding == null) {
            writer = new OutputStreamWriter(out);
        } else {
            writer = new OutputStreamWriter(out, encoding);
        }
        
        writeWrapperElementStart(writer, wrapperElement);
        
        String textContent = wrapperElement.getTextContent();
        
        if (textContent != null)
        {
            writer.write(XmlUtils.encode(textContent));
        }
        
        writer.flush();
        writer = null;
        
        if (contentBuffer != null) {
            out.write(contentBuffer, off, len);
        }
        
        writeWrapperElementEnd(out, wrapperElement);
    }
    
}
