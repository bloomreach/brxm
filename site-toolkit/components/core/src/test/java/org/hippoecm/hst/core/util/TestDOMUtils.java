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
package org.hippoecm.hst.core.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.hippoecm.hst.util.DOMElementWriter;
import org.junit.Test;

public class TestDOMUtils {

    @Test
    public void testDOM4JElement() throws Exception {
        // Tests if setTextContent() is working 
        // with our serializable element implementation which is based on dom4j

        org.w3c.dom.Element element = DOMUtils.createSerializableElement("script");
        element.setAttribute("id", "my-test-javascript");
        element.setAttribute("type", "text/javascript");
        element.setTextContent("alert('<Hello, World!>');");

        String stringified = DOMUtils.stringifyElement((org.dom4j.Element) element);

        System.out.println("stringified: " + stringified);
        assertTrue("element name is different.", stringified.contains("<script "));
        assertTrue("id attribute does not exist.", stringified.contains("id=\"my-test-javascript\""));
        assertTrue("type attribute does not exist.", stringified.contains("type=\"text/javascript\""));
        assertTrue("the text content is wrong.", stringified.contains("alert("));
        assertTrue("the text content is wrong.", stringified.contains("Hello, World!"));

        // Tests if getOwnerDocument() is working and it is possible to append text node

        element = DOMUtils.createSerializableElement("script");
        element.setAttribute("id", "my-test-javascript");
        element.setAttribute("type", "text/javascript");
        element.appendChild(element.getOwnerDocument().createTextNode("alert('<Hello, World!>');"));

        stringified = DOMUtils.stringifyElement((org.dom4j.Element) element);

        System.out.println("stringified: " + stringified);
        assertTrue("element name is different.", stringified.contains("<script "));
        assertTrue("id attribute does not exist.", stringified.contains("id=\"my-test-javascript\""));
        assertTrue("type attribute does not exist.", stringified.contains("type=\"text/javascript\""));
        assertTrue("the text content is wrong.", stringified.contains("alert("));
        assertTrue("the text content is wrong.", stringified.contains("Hello, World!"));
    }

    @Test
    public void testDOM4JWriting() throws Exception {
        org.w3c.dom.Element element = DOMUtils.createSerializableElement("script");
        element.setAttribute("id", "my-test-javascript");
        element.setAttribute("type", "text/javascript");
        element.setTextContent("alert('<Hello, World!>');");
        
        String stringified = null;
        StringWriter writer = new StringWriter(80);
        
        try {
            DOMElementWriter domWriter = new DOMElementWriter();
            domWriter.write(element, writer, 0, "  ");
        } catch (IOException e) {
        }

        stringified = writer.toString();
        System.out.println("stringified: " + stringified);
        assertTrue("element name is different.", stringified.contains("<script "));
        assertTrue("id attribute does not exist.", stringified.contains("id=\"my-test-javascript\""));
        assertTrue("type attribute does not exist.", stringified.contains("type=\"text/javascript\""));
        assertTrue("the text content is wrong.", stringified.contains("alert("));
        assertTrue("the text content is wrong.", stringified.contains("Hello, World!"));
    }
}
