/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.QID;
import static org.onehippo.cms7.autoexport.Constants.QNAME;
import static org.onehippo.cms7.autoexport.Constants.QNODE;
import static org.onehippo.cms7.autoexport.Constants.QPROPERTY;
import static org.onehippo.cms7.autoexport.Constants.QTYPE;
import static org.onehippo.cms7.autoexport.Constants.QVALUE;
import static org.onehippo.cms7.autoexport.Constants.SV_URI;
import static org.onehippo.repository.util.JcrConstants.JCR_PRIMARY_TYPE;

class Extension {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private final Module module;
    private final InitializeItemRegistry registry;
    private final File file;
    private final Document domDocument;
    private String id;
    private String encoding = DEFAULT_ENCODING;
    private volatile boolean changed = false;

    Extension(Module module, InitializeItemRegistry registry) throws Exception {
        this.module = module;
        this.file = new File(module.getExportDir(), "hippoecm-extension.xml");
        this.registry = registry;
        if (!file.exists()) {
            domDocument = createDomDocument();
        } else {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = dbf.newDocumentBuilder();
            domDocument = builder.parse(file);
            parseDomDocument();
            encoding = domDocument.getXmlEncoding();
        }
    }
    
    String getId() {
        return id;
    }
    
    void export() {
        if (changed) {
            log.info("Exporting " + file.getName() + " in module " + module.getModulePath());
            try {
                if (!file.exists()) {
                    ExportUtils.createFile(file);
                }
                final Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
                Result output = new StreamResult(file);
                Source input = new DOMSource(domDocument);
                transformer.transform(input, output);
            } catch (IOException e) {
                log.error("Exporting " + file.getName() + " failed.", e);
            } catch (TransformerConfigurationException e) {
                log.error("Exporting " + file.getName() + " failed.", e);
            } catch (TransformerException e) {
                log.error("Exporting " + file.getName() + " failed.", e);
            }
            changed = false;
        }
    }

    private Document createDomDocument() throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(getClass().getResourceAsStream("empty-hippoecm-extension.xml"));
    }

    private void parseDomDocument() {
        final Element root = domDocument.getDocumentElement();
        id = root.getAttribute(QID);
        final NodeList nodes = root.getElementsByTagName(QNODE);
        for (int i = 0; i < nodes.getLength(); i++) {
            registry.addInitializeItem(parseInitializeItemFromDomElement((Element) nodes.item(i)));
        }
    }

    private InitializeItem parseInitializeItemFromDomElement(final Element node) {
        Double sequence = null;
        String contentResource = null, contentRoot = null, nodeTypesResource = null, namespace = null;
        final String nodeName = node.getAttribute(QNAME);
        final NodeList properties = node.getElementsByTagName(QPROPERTY);
        for (int i = 0; i < properties.getLength(); i++) {
            final Element property = (Element) properties.item(i);
            final String propertyName = property.getAttribute(QNAME);
            final Element value = (Element) property.getElementsByTagName(QVALUE).item(0);
            if (propertyName.equals("hippo:contentresource")) {
                contentResource = value.getTextContent();
            }
            else if (propertyName.equals("hippo:contentroot")) {
                contentRoot = value.getTextContent();
            }
            else if (propertyName.equals("hippo:sequence")) {
                sequence = Double.parseDouble(value.getTextContent());
            }
            else if (propertyName.equals("hippo:namespace")) {
                namespace = value.getTextContent();
            }
            else if (propertyName.equals("hippo:nodetypesresource")) {
                nodeTypesResource = value.getTextContent();
            }
        }
        return new InitializeItem(nodeName, sequence, contentResource, contentRoot, null, nodeTypesResource, namespace, module.getExportDir(), module);
    }

    void initializeItemAdded(InitializeItem item) {
        domDocument.getDocumentElement().appendChild(createInitializeItemDomElement(item));
        changed = true;
    }

    void initializeItemRemoved(InitializeItem item) {
        Element element = getInitializeItemDomElement(item.getName());
        if (element != null) {
            domDocument.getDocumentElement().removeChild(element);
            changed = true;
        }
    }

    private Element getInitializeItemDomElement(String name) {
        final NodeList nodes = domDocument.getDocumentElement().getElementsByTagName(QNODE);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (element.getAttribute(QNAME).equals(name)) {
                return element;
            }
        }
        return null;
    }

    private Element createInitializeItemDomElement(InitializeItem item) {

        final Element node = domDocument.createElementNS(SV_URI, QNODE);
        node.setAttributeNS(SV_URI, QNAME, item.getName());

        Element property = createPropertyElement(JCR_PRIMARY_TYPE, "Name", "hippo:initializeitem");
        node.appendChild(property);

        property = createPropertyElement("hippo:sequence", "Double", String.valueOf(item.getSequence()));
        node.appendChild(property);

        if (item.getContentResource() != null) {
            property = createPropertyElement("hippo:contentresource", "String", item.getContentResource());
            node.appendChild(property);
        }

        if (item.getContentRoot() != null) {
            property = createPropertyElement("hippo:contentroot", "String", item.getContentRoot());
            node.appendChild(property);
        }

        if (item.getNamespace() != null) {
            property = createPropertyElement("hippo:namespace", "String", item.getNamespace());
            node.appendChild(property);
        }
        if (item.getNodeTypesResource() != null) {
            property = createPropertyElement("hippo:nodetypesresource", "String", item.getNodeTypesResource());
            node.appendChild(property);
        }

        return node;
    }

    private Element createPropertyElement(final String name, final String type, final String value) {
        final Element property;
        final Element valueElement;
        property = domDocument.createElementNS(SV_URI, QPROPERTY);
        property.setAttributeNS(SV_URI, QNAME, name);
        property.setAttributeNS(SV_URI, QTYPE, type);
        valueElement = domDocument.createElementNS(SV_URI, QVALUE);
        valueElement.appendChild(domDocument.createTextNode(value));
        property.appendChild(valueElement);
        return property;
    }

}
