package org.hippoecm.hst.core.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.hippoecm.hst.util.DOMElementWriter;
import org.junit.Ignore;
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

    //FIXME: Makes dom4j element compliant to org.w3c.dom.Element for attributes and children
    @Ignore
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
    }
}
