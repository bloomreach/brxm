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
package org.hippoecm.tools.importer.conversion;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.Context;
import org.hippoecm.tools.importer.api.Converter;
import org.hippoecm.tools.importer.api.ImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class AbstractConverter implements Converter {

    final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(AbstractConverter.class);

    public Node convert(Context context, Content content) throws IOException, RepositoryException {
        Node doc = context.createDocument(content);
        if (doc != null) {
            // set initial workflow state
            doc.setProperty("hippostd:state", "unpublished");
        }
        return doc;
    }

    public void setup(Configuration config) throws ImportException {
    }

    protected Document parse(Content source) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(source.getInputStream());
        } catch (ParserConfigurationException ex) {
            log.warn("Error parsing : " + source);
        } catch (SAXException ex) {
            log.warn("Error parsing : " + source);
        }
        return null;
    }

}
