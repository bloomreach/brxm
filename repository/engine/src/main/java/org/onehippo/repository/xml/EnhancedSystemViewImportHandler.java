/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.xml.BufferedTextValue;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.TextValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_UUID;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_MULTIPLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NAME;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NODE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_PROPERTY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_TYPE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_VALUE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.ESV_URI;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.FILE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.LOCATION;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.MERGE;

public class EnhancedSystemViewImportHandler extends DefaultHandler {

    private static Logger log = LoggerFactory.getLogger(EnhancedSystemViewImportHandler.class);

    private final Stack<Node> stack = new Stack<Node>();
    private final ContentResourceLoader contentResourceLoader;
    private final ValueFactory valueFactory;
    private final Importer importer;
    private final ImportContext importContext;
    private final InternalHippoSession resolver;

    public EnhancedSystemViewImportHandler(ImportContext importContext, InternalHippoSession session) throws RepositoryException {
        this.importer = new EnhancedSystemViewImporter(importContext.getImportTargetNode(), importContext, session);
        this.contentResourceLoader = importContext.getContentResourceLoader();
        this.importContext = importContext;
        this.valueFactory = session.getValueFactory();
        this.resolver = session;
    }

    public void warning(SAXParseException e) throws SAXException {
        log.warn("warning encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream", e);
    }

    public void error(SAXParseException e) throws SAXException {
        log.error("error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream: " + e.toString());
    }

    public void fatalError(SAXParseException e) throws SAXException {
        log.error("fatal error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream: " + e.toString());
        throw e;
    }

    @Override
    public void startDocument() throws SAXException {
        try {
            importer.start();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        final Name name = getName(namespaceURI, localName);
        if (name.equals(SV_NODE)) {
            startNode(atts);
        } else if (name.equals(SV_PROPERTY)) {
            startProperty(atts);
        } else if (name.equals(SV_VALUE)) {
            startValue(atts);
        } else {
            throw new SAXException(new InvalidSerializedDataException(
                    "Unexpected element in system view xml document: " + name));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        appendValue(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        appendValue(ch, start, length);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        final Name name = getName(namespaceURI, localName);
        if (name.equals(SV_NODE)) {
            endNode();
        } else if (name.equals(SV_PROPERTY)) {
            endProperty();
        } else if (name.equals(SV_VALUE)) {
            endValue();
        } else {
            throw new SAXException(new InvalidSerializedDataException(
                    "Unexpected element in system view xml document: " + name));
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    private void startNode(final Attributes atts) throws SAXException {
        final Node current = getCurrentNode();
        if (current != null && !current.started) {
            current.start();
        }
        stack.push(new Node(atts));
    }

    private void startProperty(final Attributes atts) throws SAXException {
        final Node node = getCurrentNode();
        if (node != null) {
            node.startProperty(atts);
        } else {
            throw new SAXException(new InvalidSerializedDataException("property definition outside node definition"));
        }
    }

    private void startValue(final Attributes atts) throws SAXException {
        final Property property = getCurrentProperty();
        if (property != null) {
            property.startValue(atts);
        } else {
            throw new SAXException(new InvalidSerializedDataException("value definition outside property definition"));
        }
    }

    private void endNode() throws SAXException {
        final Node node = getCurrentNode();
        if (node != null) {
            if (!node.started) {
                node.start();
            }
            node.end();
            stack.pop();
        }
    }

    private void endProperty() throws SAXException {
        final Property property = getCurrentProperty();
        if (property != null) {
            property.end();
        }
    }

    private void endValue() throws SAXException {
        final Value value = getCurrentValue();
        if (value != null) {
            value.end();
        }
    }

    private void appendValue(final char[] ch, final int start, final int length) throws SAXException {
        final Value value = getCurrentValue();
        if (value != null) {
            value.append(ch, start, length);
        }
    }

    private Node getCurrentNode() {
        if (!stack.isEmpty()) {
            return stack.peek();
        }
        return null;
    }

    private Property getCurrentProperty() throws SAXException {
        final Node node = getCurrentNode();
        return node != null ? node.currentProperty : null;
    }

    private Value getCurrentValue() throws SAXException {
        final Property currentProperty = getCurrentProperty();
        return currentProperty != null ? currentProperty.currentValue : null;
    }

    private static Name getName(final String namespaceURI, final String localName) {
        return NameFactoryImpl.getInstance().create(namespaceURI, localName);
    }

    private static String getAttribute(Attributes attributes, Name name) {
        return attributes.getValue(name.getNamespaceURI(), name.getLocalName());
    }

    private class Node {

        private final Name name;
        private Name type;
        private Name[] mixins;
        private NodeId uuid;
        private int index;
        private boolean started = false;
        private String merge;
        private String location;

        private Property currentProperty;
        private List<Property> properties = new ArrayList<>();

        private Node(final Attributes atts) throws SAXException {
            String svName = getAttribute(atts, SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:node"));
            }
            index = -1;
            int offset = svName.indexOf('[');
            if (offset != -1) {
                index = Integer.valueOf(svName.substring(offset+1, svName.length()-1));
                svName = svName.substring(0, offset);
            }
            try {
                name = resolver.getQName(svName);
            } catch (NameException | NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + svName, e));
            }
            merge = atts.getValue(ESV_URI, MERGE);
            location = atts.getValue(ESV_URI, LOCATION);
        }

        private void start() throws SAXException {
            try {
                final List<PropInfo> propInfos = getPropInfos();
                importer.startNode(createNodeInfo(), propInfos);
                for (PropInfo pi : propInfos) {
                    pi.dispose();
                }
                started = true;
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }

        private void end() throws SAXException {
            try {
                importer.endNode(createNodeInfo());
            } catch (RepositoryException e) {
                throw new SAXException(e);
            }
        }

        private EnhancedNodeInfo createNodeInfo() {
            return new EnhancedNodeInfo(name, type, mixins, uuid, merge, location, index);
        }

        private void startProperty(Attributes atts) throws SAXException {
            currentProperty = new Property(this);
            currentProperty.start(atts);
            properties.add(currentProperty);
        }

        private List<PropInfo> getPropInfos() {
            final List<PropInfo> infos = new ArrayList<>();
            for (Property property : properties) {
                if (property.info != null) {
                    infos.add(property.info);
                }
            }
            return infos;
        }
    }

    private class Property {

        private final Node node;

        private Name name;
        private int type;
        private Boolean multiple;
        private String merge;
        private String location;

        private Value currentValue;
        private List<Value> values = new ArrayList<>();

        private EnhancedPropInfo info;

        private Property(final Node node) {
            this.node = node;
        }

        private void start(final Attributes atts) throws SAXException {
            String svName = getAttribute(atts, SV_NAME);
            if (StringUtils.isEmpty(svName)) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:property"));
            }
            try {
                name = resolver.getQName(svName);
            } catch (NameException | NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + svName, e));
            }
            String strType = getAttribute(atts, SV_TYPE);
            if (StringUtils.isEmpty(strType)) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:type attribute of element sv:property"));
            }
            try {
                type = PropertyType.valueFromName(strType);
            } catch (IllegalArgumentException e) {
                throw new SAXException(new InvalidSerializedDataException("Unknown property type: " + strType, e));
            }
            String strMultiple = getAttribute(atts, SV_MULTIPLE);
            if (!StringUtils.isEmpty(strMultiple)) {
                multiple = Boolean.valueOf(strMultiple);
            }
            merge = atts.getValue(ESV_URI, MERGE);
            location = atts.getValue(ESV_URI, LOCATION);
        }

        private void end() throws SAXException {
            if (name.equals(JCR_PRIMARYTYPE)) {
                setPrimaryType();
            } else if (name.equals(JCR_MIXINTYPES)) {
                setMixinTypes();
            } else if (name.equals(JCR_UUID)) {
                setUuid();
            } else {
                createPropInfo();
            }
        }

        private void createPropInfo() {
            info = new EnhancedPropInfo(resolver, name, type, multiple, getTextValues(), merge, location,
                    getURLValues(), valueFactory, importContext);
        }

        private void setUuid() throws SAXException {
            if(!values.isEmpty()) {
                String strUuid = null;
                try {
                    strUuid = values.get(0).text.retrieve();
                    node.uuid = NodeId.valueOf(strUuid);
                } catch (IOException e) {
                    throw new SAXException("error while retrieving value", e);
                } catch (IllegalArgumentException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal uuid: " + strUuid, e));
                }
            } else {
                throw new SAXException(new InvalidSerializedDataException("missing value for property jcr:uuid"));
            }
        }

        private void setMixinTypes() throws SAXException {
            if (!values.isEmpty()) {
                node.mixins = new Name[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    String strName = null;
                    try {
                        strName = values.get(i).text.retrieve();
                        node.mixins[i] = resolver.getQName(strName);
                    } catch (IOException ioe) {
                        throw new SAXException("error while retrieving value", ioe);
                    } catch (NameException | NamespaceException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + strName, e));
                    }
                }
            }
        }

        private void setPrimaryType() throws SAXException {
            if (!values.isEmpty()) {
                String strName = null;
                try {
                    strName = values.get(0).text.retrieve();
                    node.type = resolver.getQName(strName);
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                } catch (NameException | NamespaceException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + strName, e));
                }
            } else {
                throw new SAXException("missing value for property jcr:primaryType");
            }
        }

        private void startValue(Attributes atts) throws SAXException {
            currentValue = new Value(this);
            currentValue.start(atts);
            values.add(currentValue);
        }

        private TextValue[] getTextValues() {
            final List<TextValue> result = new ArrayList<>();
            for (Value value : values) {
                if (value.text != null) {
                    result.add(value.text);
                }
            }
            return result.toArray(new TextValue[result.size()]);
        }

        private URL[] getURLValues() {
            final List<URL> result = new ArrayList<>();
            for (Value value : values) {
                if (value.url != null) {
                    result.add(value.url);
                }
            }
            return result.toArray(new URL[result.size()]);
        }
    }

    private class Value {
        private final Property property;
        private URL url;
        private BufferedTextValue text;

        private Value(final Property property) {
            this.property = property;
        }

        private void start(final Attributes atts) throws SAXException {
            final String fileName = atts.getValue(ESV_URI, FILE);
            if (fileName != null) {
                try {
                    url = contentResourceLoader != null ? contentResourceLoader.getResource(fileName) : null;
                    if (url == null) {
                        throw new SAXException("Missing file resource: " + fileName);
                    }
                } catch (MalformedURLException e) {
                    throw new SAXException("Malformed file resource path: " + fileName);
                }
            } else {
                text = new BufferedTextValue(resolver, ValueFactoryImpl.getInstance());
                String xsiType = atts.getValue("xsi:type");
                text.setBase64("xs:base64Binary".equals(xsiType));
            }
        }

        private void end() {
            property.currentValue = null;
        }

        private void append(final char[] ch, final int start, final int length) throws SAXException {
            if (text != null) {
                try {
                    text.append(ch, start, length);
                } catch (IOException ioe) {
                    throw new SAXException("error while processing property value", ioe);
                }
            }
        }

    }

}
