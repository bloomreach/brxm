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
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.AE_ID_QNAME;
import static org.onehippo.cms7.autoexport.Constants.NAME_QNAME;
import static org.onehippo.cms7.autoexport.Constants.NODE_QNAME;
import static org.onehippo.cms7.autoexport.Constants.PROPERTY_QNAME;
import static org.onehippo.cms7.autoexport.Constants.TYPE_QNAME;
import static org.onehippo.cms7.autoexport.Constants.VALUE_QNAME;

class Extension {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private final Module module;
    private final InitializeItemRegistry registry;
    private final File file;
    private final Document document;
    private final OutputFormat format;
    private String id;
    private volatile boolean changed = false;

    Extension(Module module, InitializeItemRegistry registry) throws DocumentException {
        this.module = module;
        this.file = new File(module.getExportDir(), "hippoecm-extension.xml");
        this.registry = registry;
        String encoding = DEFAULT_ENCODING;
        if (!file.exists()) {
            document = createDocument();
        } else {
            SAXReader reader = new SAXReader();
            document = reader.read(file);
            parseDocument();
            encoding = document.getXMLEncoding();
        }
        format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(false);
        format.setEncoding(encoding);
    }
    
    String getId() {
        return id;
    }
    
    void export() {
        if (changed) {
            log.info("Exporting " + file.getName() + " in module " + module.getModulePath());
            XMLWriter writer = null;
            try {
                if (!file.exists()) {
                    ExportUtils.createFile(file);
                }
                writer = new XMLWriter(new FileWriter(file), format);
                writer.write(document);
                writer.flush();
            } catch (IOException e) {
                log.error("Exporting " + file.getName() + " failed.", e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        log.error("Failed to close xml writer while exporting extension", e);
                    }
                }
            }
            changed = false;
        }
    }
    
    /*
     * Creates empty hippoecm-extension.xml:
     * <sv:node xmlns:sv="http://www.jcp.org/jcr/sv/1.0" sv:name="hippo:initialize">
     *   <sv:property sv:name="jcr:primaryType" sv:type="Name">
     *     <sv:value>hippo:initializefolder</sv:value>
     *   </sv:property>
     * </sv:node>
     */
    private Document createDocument() {
        Document document = DocumentFactory.getInstance().createDocument();
        Element root = DocumentFactory.getInstance().createElement(NODE_QNAME);
        root.add(DocumentFactory.getInstance().createAttribute(root, NAME_QNAME, "hippo:initialize"));
        Element property = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        property.add(DocumentFactory.getInstance().createAttribute(property, NAME_QNAME, "jcr:primaryType"));
        property.add(DocumentFactory.getInstance().createAttribute(property, TYPE_QNAME, "Name"));
        Element value = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        value.setText("hippo:initializefolder");
        property.add(value);
        root.add(property);
        document.setRootElement(root);
        return document;
    }
    
    private void parseDocument() {
        Element root = document.getRootElement();
        id = root.attributeValue(AE_ID_QNAME);
        for (Object o : root.elements(NODE_QNAME)) {
            registry.addInitializeItem(parseInitializeItem((Element) o));
        }
    }
    
    private InitializeItem parseInitializeItem(Element element) {
        String name = element.attributeValue(NAME_QNAME);
        Double sequence = null;
        String contentResource = null, contentRoot = null, nodeTypesResource = null, namespace = null;
        for (Object o : element.elements()) {
            Element property = (Element) o;
            String propName = property.attributeValue(NAME_QNAME);
            if (propName.equals("hippo:contentresource")) {
                contentResource = property.element(VALUE_QNAME).getText();
            } else if (propName.equals("hippo:contentroot")) {
                contentRoot = property.element(VALUE_QNAME).getText();
            } else if (propName.equals("hippo:sequence")) {
                sequence = Double.parseDouble(property.element(VALUE_QNAME).getText());
            } else if (propName.equals("hippo:namespace")) {
                namespace = property.element(VALUE_QNAME).getText();
            } else if (propName.equals("hippo:nodetypesresource")) {
                nodeTypesResource = property.element(VALUE_QNAME).getText();
            }
        }
        return new InitializeItem(name, sequence, contentResource, contentRoot, null, nodeTypesResource, namespace, module.getExportDir(), module);
    }

    void initializeItemAdded(InitializeItem item) {
        document.getRootElement().add(createInitializeItemElement(item));
        changed = true;
    }

    void initializeItemRemoved(InitializeItem item) {
        Element element = getInitializeItemElement(item.getName());
        if (element != null) {
            document.getRootElement().remove(element);
            changed = true;
        }
    }
    
    void initializeItemReplaced(InitializeItem oldItem, InitializeItem newItem) {
        Element element = getInitializeItemElement(oldItem.getName());
        if (element != null) {
            for (Object o : element.elements()) {
                if (((Element) o).attributeValue(NAME_QNAME).equals("hippo:namespace")) {
                    ((Element) o).element(VALUE_QNAME).setText(newItem.getNamespace());
                    changed = true;
                }
            }
        }
    }
    
    private Element getInitializeItemElement(String name) {
        for (Object o : document.getRootElement().elements()) {
            Element element = (Element) o;
            if (((Element) o).attributeValue(NAME_QNAME).equals(name)) {
                return element;
            }
        }
        return null;
    }
    
    private Element createInitializeItemElement(InitializeItem item) {
        // create element:
        // <sv:node sv:name="{item.getName()}"/>
        Element initializeItemElement = DocumentFactory.getInstance().createElement(NODE_QNAME);
        initializeItemElement.add(DocumentFactory.getInstance().createAttribute(initializeItemElement, NAME_QNAME, item.getName()));
        // create element:
        // <sv:property sv:name="jcr:primaryType" sv:type="Name">
        //   <sv:value>hippo:initializeitem</sv:value>
        // </sv:property>
        Element primaryTypeProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        primaryTypeProperty.add(DocumentFactory.getInstance().createAttribute(primaryTypeProperty, NAME_QNAME, "jcr:primaryType"));
        primaryTypeProperty.add(DocumentFactory.getInstance().createAttribute(primaryTypeProperty, TYPE_QNAME, "Name"));
        Element primaryTypePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        primaryTypePropertyValue.setText("hippo:initializeitem");
        primaryTypeProperty.add(primaryTypePropertyValue);
        initializeItemElement.add(primaryTypeProperty);
        // create element:
        // <sv:property sv:name="hippo:sequence" sv:type="Double">
        //   <sv:value>{item.getSequence()}</sv:value>
        // </sv:property>
        Element sequenceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        sequenceProperty.add(DocumentFactory.getInstance().createAttribute(sequenceProperty, NAME_QNAME, "hippo:sequence"));
        sequenceProperty.add(DocumentFactory.getInstance().createAttribute(sequenceProperty, TYPE_QNAME, "Double"));
        Element sequencePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        sequencePropertyValue.setText(String.valueOf(item.getSequence()));
        sequenceProperty.add(sequencePropertyValue);
        initializeItemElement.add(sequenceProperty);
        
        if (item.getContentResource() != null) {
            // create element:
            // <sv:property sv:name="hippo:contentresource" sv:type="String">
            //   <sv:value>{item.getContentResource()}</sv:value>
            // </sv:property>
            Element contentResourceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            contentResourceProperty.add(DocumentFactory.getInstance().createAttribute(contentResourceProperty, NAME_QNAME, "hippo:contentresource"));
            contentResourceProperty.add(DocumentFactory.getInstance().createAttribute(contentResourceProperty, TYPE_QNAME, "String"));
            Element contentResourcePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            contentResourcePropertyValue.setText(item.getContentResource());
            contentResourceProperty.add(contentResourcePropertyValue);
            initializeItemElement.add(contentResourceProperty);
        }

        if (item.getContentRoot() != null) {
            // create element:
            // <sv:property sv:name="hippo:contentroot" sv:type="String">
            //   <sv:value>{item.getContentRoot()}</sv:value>
            // </sv:property>
            Element contentRootProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            contentRootProperty.add(DocumentFactory.getInstance().createAttribute(contentRootProperty, NAME_QNAME, "hippo:contentroot"));
            contentRootProperty.add(DocumentFactory.getInstance().createAttribute(contentRootProperty, TYPE_QNAME, "String"));
            Element contentRootPropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            contentRootPropertyValue.setText(item.getContentRoot());
            contentRootProperty.add(contentRootPropertyValue);
            initializeItemElement.add(contentRootProperty);
        }

        if (item.getNamespace() != null) {
            // create element:
            // <sv:property sv:name="hippo:namespace" sv:type="String">
            //   <sv:value>{this.m_namespace}</sv:value>
            // </sv:property>
            Element namespaceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, NAME_QNAME, "hippo:namespace"));
            namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, TYPE_QNAME, "String"));
            Element namespacePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            namespacePropertyValue.setText(item.getNamespace());
            namespaceProperty.add(namespacePropertyValue);
            initializeItemElement.add(namespaceProperty);
        }
        
        if (item.getNodeTypesResource() != null) {
            // create element:
            // <sv:property sv:name="hippo:nodetypesresource" sv:type="String">
            //   <sv:value>{this.m_file.getName()}</sv:value>
            // </sv:property>
            Element cndProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            cndProperty.add(DocumentFactory.getInstance().createAttribute(cndProperty, NAME_QNAME, "hippo:nodetypesresource"));
            cndProperty.add(DocumentFactory.getInstance().createAttribute(cndProperty, TYPE_QNAME, "String"));
            Element cndPropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            cndPropertyValue.setText(item.getNodeTypesResource());
            cndProperty.add(cndPropertyValue);
            initializeItemElement.add(cndProperty);
        }
        
        return initializeItemElement;

    }

}
