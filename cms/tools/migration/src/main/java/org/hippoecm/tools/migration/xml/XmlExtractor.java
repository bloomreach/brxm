/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
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
package org.hippoecm.tools.migration.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Extract contents from a xml file and store the extracted properties in a map.
 * The extraction rules are set with the ExtractorInstructions
 */
public class XmlExtractor {

    /** List with the ExtractorInstructions */
    final List instructions;

    /**
     * Set the ExtractorInstructions
     * @param instructions
     */
    public XmlExtractor(List instructions) {
        this.instructions = instructions;
    }

    /**
     * Extract the properties from the xml content stream
     * @param content
     * @return a map with the extracted properties
     * @throws ExtractorException
     */
    public Map extract(InputStream content) throws ExtractorException {
        Map properties = new HashMap();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document d = db.parse(content);
            for (Iterator i = instructions.iterator(); i.hasNext();) {
                ExtractorInstruction instruction = (ExtractorInstruction) i.next();
                XObject x = XPathAPI.eval(d, "string(" + instruction.getXPath() + ")");
                if (x instanceof XString) {
                    properties.put(instruction.getName(), ((XString) x).toString());
                } else {
                    properties.put(instruction.getName(), "");
                }

            }
        } catch (IOException e) {
            throw new ExtractorException("Exception while extracting content: " + e.getMessage());
        } catch (SAXException e) {
            throw new ExtractorException("Exception while extracting content: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new ExtractorException("Exception while extracting content: " + e.getMessage());
        } catch (TransformerException e) {
            throw new ExtractorException("Exception while extracting content: " + e.getMessage());
        }
        return properties;
    }

}
