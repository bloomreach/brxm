/*
 *  Copyright 2010 Hippo.
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
package org.outerj.daisy.diff;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.junit.Test;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class TestDaisyDiff {
    
    @Test
    public void testDiff() throws SAXException, IOException, TransformerConfigurationException {
        InputSource oldSource = new InputSource();
        oldSource.setCharacterStream(new StringReader("<html><body>aap</body></html>"));

        InputSource newSource = new InputSource();
        newSource.setCharacterStream(new StringReader("<html><body>noot</body></html>"));

        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler handler = tf.newTransformerHandler();
        DOMResult dr = new DOMResult();
        handler.setResult(dr);
        handler.startDocument();
        handler.startElement(null, "html", "html", new AttributesImpl());
        DaisyDiff.diffHTML(oldSource, newSource, handler, null, null);
        handler.endElement(null, "html", "html");
        handler.endDocument();

        Document doc = (Document) dr.getNode();
        NodeList spans = doc.getElementsByTagName("span");
        assertEquals(2, spans.getLength());

        CharacterData data = (CharacterData) spans.item(0).getFirstChild();
        assertEquals("aap", data.getData());

        data = (CharacterData) spans.item(1).getFirstChild();
        assertEquals("noot", data.getData());
    }

}
