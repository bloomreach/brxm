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
package org.hippoecm.hst.core.util;

import java.io.IOException;
import java.io.StringWriter;

import org.hippoecm.hst.util.DOMElementWriter;
import org.w3c.dom.Element;

/**
 * DOMUtils
 * 
 * @version $Id$
 */
public class DOMUtils {

    public static final int DEFAULT_ELEMENT_STRINGIFYING_BUFFER_SIZE = 80;
    public static final int DEFAULT_INDENT = 0;
    public static final String DEFAULT_INDENT_WITH = "\t";

    private DOMUtils() {
    }

    public static String stringifyElement(Element element) {
        return stringifyElement(element, DEFAULT_ELEMENT_STRINGIFYING_BUFFER_SIZE, DEFAULT_INDENT, DEFAULT_INDENT_WITH);
    }

    public static String stringifyElement(Element element, int initialBufferSize, int indent, String indentWith) {
        String stringified = null;
        StringWriter writer = new StringWriter(initialBufferSize);

        try {
            DOMElementWriter domWriter = new DOMElementWriter();
            domWriter.write(element, writer, indent, indentWith);
        } catch (IOException e) {
        }

        stringified = writer.toString();
        return stringified;
    }

    public static String stringifyElementToHtml(Element element) {
        return stringifyElement(element);
    }

    public static String getIdAttribute(Element element) {
        String value = null;

        if (element != null) {
            if (element.hasAttribute("id"))
                value = element.getAttribute("id");
            else if (element.hasAttribute("ID"))
                value = element.getAttribute("ID");
            else if (element.hasAttribute("Id"))
                value = element.getAttribute("Id");
            else if (element.hasAttribute("iD"))
                value = element.getAttribute("iD");
        }

        return value;
    }

}
