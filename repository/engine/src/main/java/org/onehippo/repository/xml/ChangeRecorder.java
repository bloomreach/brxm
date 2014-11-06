/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.value.ValueHelper;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.onehippo.repository.xml.ResultConstants.MERGEPROP;
import static org.onehippo.repository.xml.ResultConstants.NEWNODE;
import static org.onehippo.repository.xml.ResultConstants.ID;
import static org.onehippo.repository.xml.ResultConstants.MERGENODE;
import static org.onehippo.repository.xml.ResultConstants.MIXIN;
import static org.onehippo.repository.xml.ResultConstants.MULTI;
import static org.onehippo.repository.xml.ResultConstants.NAME;
import static org.onehippo.repository.xml.ResultConstants.NEWPROP;
import static org.onehippo.repository.xml.ResultConstants.PTYPE;
import static org.onehippo.repository.xml.ResultConstants.RESULT;
import static org.onehippo.repository.xml.ResultConstants.TYPE;
import static org.onehippo.repository.xml.ResultConstants.VAL;

class ChangeRecorder {

    private static final String CDATA = "CDATA";
    
    private ContentHandler handler;
    private final NameResolver resolver;
    private final Map<String, NodeInfo> added = new HashMap<>();
    private final Map<String, NodeInfo> merged = new HashMap<>();

    ChangeRecorder(final NameResolver resolver) {
        this.resolver = resolver;
    }

    void exportResult(final OutputStream out) throws RepositoryException {
        try {
            TransformerHandler transformerHandler = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
            Transformer transformer = transformerHandler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformerHandler.setResult(new StreamResult(out));
            handler = new DefaultContentHandler(transformerHandler);
            handler.startDocument();
            startElement(RESULT);
            emitAddedNodes();
            emitMergedNodes();
            endElement(RESULT);
            handler.endDocument();
        } catch (IOException | SAXException | TransformerConfigurationException e) {
            throw new RepositoryException(e);
        }
    }

    private void endElement(final String element) throws SAXException {
        handler.endElement(null, element, element);
    }

    private void startElement(final String element) throws SAXException {
        startElement(element, null);
    }

    private void startElement(final String element, Attributes attributes) throws SAXException {
        handler.startElement(null, element, element, attributes);
    }

    private void emitMergedNodes() throws SAXException, RepositoryException, IOException {
        for (NodeInfo nodeInfo : merged.values()) {
            if (nodeInfo.hasChanges()) {
                emitMergedNode(nodeInfo);
            }
        }
    }

    private void emitMergedNode(final NodeInfo nodeInfo) throws SAXException, RepositoryException, IOException {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, ID, ID, CDATA, nodeInfo.identifier);
        startElement(MERGENODE, attributes);
        emitProperties(nodeInfo.properties);
        emitMixins(nodeInfo.mixins);
        emitPrimaryType(nodeInfo.primaryType);
        endElement(MERGENODE);
    }

    private void emitPrimaryType(final Name primaryTypeName) throws SAXException, NamespaceException {
        if (primaryTypeName != null) {
            startElement(PTYPE);
            final String primaryType = resolver.getJCRName(primaryTypeName);
            handler.characters(primaryType.toCharArray(), 0, primaryType.length());
            endElement(PTYPE);
        }
    }

    private void emitMixins(final Collection<Name> mixins) throws SAXException, NamespaceException {
        if (mixins != null) {
            for (Name mixin : mixins) {
                emitMixin(mixin);
            }
        }
    }

    private void emitMixin(final Name mixinName) throws SAXException, NamespaceException {
        startElement(MIXIN);
        final String mixin = resolver.getJCRName(mixinName);
        handler.characters(mixin.toCharArray(), 0, mixin.length());
        endElement(MIXIN);
    }

    private void emitProperties(final Collection<PropertyInfo> properties) throws SAXException, RepositoryException, IOException {
        if (properties != null) {
            for (PropertyInfo property : properties) {
                emitProperty(property);
            }
        }
    }

    private void emitProperty(final PropertyInfo propertyInfo) throws SAXException, RepositoryException, IOException {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, NAME, NAME, CDATA, resolver.getJCRName(propertyInfo.name));
        if (propertyInfo.values == null) {
            startElement(NEWPROP, attributes);
            endElement(NEWPROP);
        } else {
            attributes.addAttribute(null, TYPE, TYPE, CDATA, String.valueOf(propertyInfo.type));
            if (propertyInfo.multiple) {
                attributes.addAttribute(null, MULTI, MULTI, CDATA, Boolean.TRUE.toString());
            }
            startElement(MERGEPROP, attributes);
            for (Value oldValue : propertyInfo.values) {
                emitValue(oldValue);
            }
            endElement(MERGEPROP);
        }
    }

    private void emitValue(final Value value) throws SAXException, RepositoryException, IOException {
        startElement(VAL);
        Writer writer = new Writer() {
            @Override
            public void close() {}

            @Override
            public void flush() {}

            @Override
            public void write(char[] chars, int off, int len) throws IOException {
                try {
                    handler.characters(chars, off, len);
                } catch (SAXException se) {
                    throw new IOException(se.toString());
                }
            }
        };
        ValueHelper.serialize(value, false, false, writer);
        endElement(VAL);
    }


    private void emitAddedNodes() throws SAXException {
        for (NodeInfo nodeInfo : added.values()) {
            emitAddedNode(nodeInfo);
        }
    }

    private void emitAddedNode(final NodeInfo nodeInfo) throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, ID, ID, CDATA, nodeInfo.identifier);
        startElement(NEWNODE, attributes);
        endElement(NEWNODE);
    }

    void nodeAdded(final Node node) throws RepositoryException {
        if (!ancestorAdded(node)) {
            added.put(node.getIdentifier(), new NodeInfo(node.getIdentifier()));
        }
    }

    private boolean ancestorAdded(final Node node) throws RepositoryException {
        for (String id : added.keySet()) {
            if (isAncestor(id, node)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAncestor(final String id, final Node node) throws RepositoryException {
        try {
            final Node parent = node.getParent();
            return parent.getIdentifier().equals(id) || isAncestor(id, parent);
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    void nodeMerged(final Node node) throws RepositoryException {
        merged.put(node.getIdentifier(), new NodeInfo(node.getIdentifier()));
    }

    void propertySet(final String id, final Name name, final Value[] oldValues, final Boolean oldMultiple, final int oldType) {
        final NodeInfo nodeInfo = merged.get(id);
        if (nodeInfo != null) {
            nodeInfo.addProperty(new PropertyInfo(name, oldValues, oldMultiple, oldType));
        }
    }

    void mixinsSet(final String identifier, final Collection<Name> oldMixins) {
        merged.get(identifier).mixins = oldMixins;
    }

    void primaryTypeSet(final String identifier, final Name oldPrimaryType) {
        merged.get(identifier).primaryType = oldPrimaryType;
    }

    boolean isMerged() {
        return !merged.isEmpty();
    }

    private final static class NodeInfo {
        private String identifier;
        private Collection<PropertyInfo> properties;
        private Collection<Name> mixins;
        private Name primaryType;

        private NodeInfo(final String identifier) {
            this.identifier = identifier;
        }

        private boolean hasChanges() {
            return properties != null || mixins != null || primaryType != null;
        }

        private void addProperty(PropertyInfo propertyInfo) {
            if (properties == null) {
                properties = new ArrayList<>();
            }
            properties.add(propertyInfo);
        }
    }

    private final static class PropertyInfo {
        private final Name name;
        private final boolean multiple;
        private final Value[] values;
        private final int type;

        private PropertyInfo(final Name name, final Value[] values, final boolean multiple, final int type) {
            this.name = name;
            this.values = values;
            this.multiple = multiple;
            this.type = type;
        }
    }
}
