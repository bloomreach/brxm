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
package org.hippoecm.tools.importer;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.hippoecm.repository.api.ISO9075Helper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Simple example implementation. This implementation is not 
 * intended for direct usage, but for extending. The methods 
 * convertDocToJCR and getNodeType should be overwritten with a
 * content specific implementation.
 */
public class SimpleXmlImporter implements ContentImporter {

    final static String SVN_ID = "$Id$";
    
    private static Logger log = Logger.getLogger(SimpleXmlImporter.class);
    
    private boolean overwrite = true;
    
    public void setup(Configuration config) throws RepositoryException {
        overwrite = config.getBoolean("overwrite", true);
        log.info("Overwriting existing content: " + overwrite);
    }
    

    public void convertDocToJCR(Node parent, String fileName, InputStream content) throws RepositoryException, IOException {

        String name = fileName;
        if (fileName.endsWith(".xml")) {
            name = fileName.substring(0, fileName.length() - 4);
        }

        String title = null;
        String body = null;
        String introduction = null;
        
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db;
        try {
            NodeList nodes;
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(content);
            

            nodes = doc.getElementsByTagName("title");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element titleElem = (Element) nodes.item(i);
                org.w3c.dom.Node childNode = titleElem.getFirstChild();
                if (childNode instanceof Text) {
                    title = childNode.getNodeValue();
                }
            }

            nodes = doc.getElementsByTagName("summary");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element titleElem = (Element) nodes.item(i);
                org.w3c.dom.Node childNode = titleElem.getFirstChild();
                if (childNode instanceof Text) {
                    introduction = childNode.getNodeValue();
                }
            }
            nodes = doc.getElementsByTagName("body");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element titleElem = (Element) nodes.item(i);
                org.w3c.dom.Node childNode = titleElem.getFirstChild();
                if (childNode instanceof Text) {
                    body = childNode.getNodeValue();
                }
            }

            
        } catch (ParserConfigurationException e) {
            log.warn("Error parsing : " + buildPath(parent, name));
            return;
        } catch (SAXException e) {
            log.warn("Error parsing : " + buildPath(parent, name));
            return;
        }


        // build document
        Node doc = createDocument(parent, name, getNodeType(fileName));
        if (title == null ) {
            log.warn("Title not found, skipping : " + buildPath(parent, name));
            return;
        } else {
            doc.setProperty("defaultcontent:title", title);
        }
        if (body == null) {
            log.warn("Body not found, skipping : " + buildPath(parent, name));
            return;
        } else {
            doc.setProperty("defaultcontent:body", body);
        }
        if (introduction == null) {
            log.warn("Introduction not found, skipping : " + buildPath(parent, name));
            return;
        } else {
            doc.setProperty("defaultcontent:introduction", introduction);
        }
        
    }
    
    /**
     * Create a document with handle
     * @param parent
     * @param name the name will be encode
     * @param type
     * @return the handle
     * @throws RepositoryException
     */
    public Node createDocument(Node parent, String name, String type) throws RepositoryException {
        String encoded = ISO9075Helper.encodeLocalName(nameTranslate(name));
        
        if (parent.hasNode(encoded)) {
            if (overwrite) {
                log.info("Removing document: " + buildPath(parent, encoded));
                parent.getNode(encoded).remove();
            } else {
                log.info("Skipping document: " + buildPath(parent, encoded));
                return parent.getNode(encoded);
            }
        }
        log.info("Creating document: " + buildPath(parent, encoded));
        
        // add handle
        Node handle = parent.addNode(encoded, "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        
        // add document node
        Node document = handle.addNode(encoded, getNodeType(name));
        document.addMixin("hippo:harddocument");
        
        // set initial workflow state
        document.setProperty("hippostd:state", "unpublished");
        
        return handle;
    }

    /**
     * Create a folder
     * @param parent
     * @param name the name will be encode
     * @return the folder node
     * @throws RepositoryException
     */
    public Node createFolder(Node parent, String name) throws RepositoryException {
        String encoded = ISO9075Helper.encodeLocalName(nameTranslate(name));
        if (parent.hasNode(encoded)) {
            if (overwrite) {
                log.info("Removing folder: " + buildPath(parent, encoded));
                parent.getNode(encoded).remove();
            } else {
                log.info("Skipping folder: " + buildPath(parent, encoded));
                return parent.getNode(encoded);
            }
        }
        log.info("Creating folder: " + buildPath(parent, encoded));
        Node folder = parent.addNode(encoded, "hippostd:folder");
        folder.addMixin("hippo:harddocument");
        return folder;
    }
    
    /*
     * {@inheritDoc}
     */
    public Node createPath(Node rootNode, String path) throws RepositoryException {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if ("".equals(path) || "/".equals(path)) {
            return rootNode;
        } else {
            String[] elements = path.split("\\/");
            Node parent = rootNode;
            for (int i = 0; i < elements.length; i++) {
                if (parent.hasNode(ISO9075Helper.encodeLocalName(elements[i]))) {
                    parent = parent.getNode(ISO9075Helper.encodeLocalName(elements[i]));
                } else {
                    parent = createFolder(parent, elements[i]);
                }
            }
            return parent;
        }
    }

    /**
     * Helper method to get the document type based on the filename
     */
    public String getNodeType(String fileName) {
        return "defaultcontent:article";
    }

    /**
     * Translate names of documents and path elements. This is useful for 
     * i18n translations like "france" -> "frankrijk".
     */
    public String nameTranslate(String name) {
        return name;
    }
    
    /**
     * Skip the specific file path?
     */
    public boolean skipPath(String path) {
        if (path.endsWith("system")) {
            return true;
        }
        return false;
    }

    /**
     * Helper method for creating absolute paths, used for logging.
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    final public String buildPath(Node parent, String name) throws RepositoryException {
        StringBuffer buf = new StringBuffer();
        if (parent.getDepth() > 0) {
            buf.append(parent.getPath());
        }
        buf.append('/').append(name);
        return buf.toString();
    }
}
