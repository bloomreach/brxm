/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImportTest extends RepositoryTestCase {

    byte[] data;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.getRootNode().addNode("test", "nt:unstructured");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        super.tearDown();
    }

    private void testWhiteSpacesInBinary(byte[] data) throws Exception {
        if(data.length > 255) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte)(i &0xff);
                data[i] = (byte)((i>>8) & 0xff);
            }
        } else {
            for (byte i = 0; i < data.length; i++)
                data[i] = i;
        }

        Node test = session.getRootNode().getNode("test");
        Node resource = test.addNode("data", "nt:resource");
        resource.setProperty("jcr:data", session.getValueFactory().createValue(new ByteArrayBinary(data)));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        session.save();
        File file = new File("import.xml");
        //FileOutputStream out = new FileOutputStream(file);
        //session.exportSystemView("/test/data", out, false, false);
        session.exportSystemView("/test/data", new OutputXML(file), false, false);

        FileInputStream in = new FileInputStream(new File("import.xml"));
        session.importXML(test.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        in.close();
        session.save();

        assertTrue(test.hasNode("data[2]"));
        Binary binary = test.getNode("data[2]").getProperty("jcr:data").getBinary();
        assertEquals(data.length, binary.getSize());
        byte[] compareData = new byte[data.length];
        assertEquals(data.length, binary.read(compareData, 0));
        for(int i=0; i<data.length; i++)
            assertEquals(data[i], compareData[i]);
    }

    @Test
    public void testWhiteSpacesInSmallBinary() throws Exception {
        data = new byte[32];
        testWhiteSpacesInBinary(data);
    }

    @Test
    public void testWhiteSpacesInLargeBinary() throws Exception {
        data = new byte[1024*125];
        testWhiteSpacesInBinary(data);
    }
    
    @Test
    public void testNoFailureWithFaultyNamespaceDeclaration() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("import/faulty-namespace.xml");
        session.importXML("/test", in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    }

    static class ByteArrayBinary implements Binary {

        byte[] data;

        public ByteArrayBinary(byte[] array) {
            this.data = array;
        }

        public InputStream getStream() throws RepositoryException {
            return new ByteArrayInputStream(data);
        }

        public int read(byte[] b, long position) throws IOException, RepositoryException {
            int len = b.length;
            if (len > data.length - (int) position) {
                len = data.length - (int) position;
            }
            System.arraycopy(data, (int) position, b, 0, b.length);
            return len;
        }

        public long getSize() throws RepositoryException {
            return data.length;
        }

        public void dispose() {
        }
    }

    static class OutputXML implements ContentHandler {

        PrintStream output;
        StringBuffer buffer = new StringBuffer();
        int indent = 0;
        boolean start = true;

        OutputXML(File file) throws IOException {
            output = new PrintStream(new FileOutputStream(file));
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
            output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }

        public void endDocument() throws SAXException {
            output.close();
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }
        boolean isBinary = false;

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            for (int i = 0; i < indent; i++) {
                output.print("  ");
            }
            output.print("<sv:" + localName);
            if (start) {
                output.print(" xmlns:sv=\"" + uri + "\"");
                start = false;
            }
            for (int i = 0; i < atts.getLength(); i++) {
                output.print(" sv:" + atts.getLocalName(i) + "=\"" + atts.getValue(i) + "\"");
                if (atts.getLocalName(i).equals("type") && atts.getValue(i).equals("Binary")) {
                    isBinary = true;
                }
            }
            output.print(">");
            if (!localName.equals("value")) {
                output.println();
            }
            buffer = new StringBuffer();
            ++indent;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            --indent;
            if (!localName.equals("value")) {
                for (int i = 0; i < indent; i++) {
                    output.print("  ");
                }
            }
            if (isBinary) {
                // output.print(" \n\t  " + new String(buffer) + "  \n\t  ");
                int len = buffer.length();
                for(int i=buffer.length(); i>0; i--)
                    buffer.insert(i, " \n\t ");
                output.print(" \n\t  " + new String(buffer) + "  \n\t  ");
            } else {
                output.print(new String(buffer));
            }
            output.println("</sv:" + localName + ">");
            buffer = new StringBuffer();
            isBinary = false;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            buffer.append(ch, start, length);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }
    }
}
