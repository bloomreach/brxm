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
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.xml.TextValue;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static org.onehippo.repository.xml.ResultConstants.*;

public class RevertImportHandler extends DefaultHandler {

    private static final Logger log = LoggerFactory.getLogger(RevertImportHandler.class);

    private final InternalHippoSession session;
    private Node node;
    private Property property;
    private List<TextValue> values;
    private List<String> mixins;
    private BufferedStringValue type;
    private BufferedStringValue value;
    private Boolean multiple;

    public RevertImportHandler(final InternalHippoSession session) {
        this.session = session;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {
        switch (localName) {
            case ADD:
                removeNode(attributes.getValue(ID));
                break;
            case MERGE:
                lookupNode(attributes.getValue(ID));
                break;
            case PROP:
                final String name = attributes.getValue(NAME);
                if (BooleanUtils.toBoolean(attributes.getValue(NEW))) {
                    removeProperty(name);
                } else {
                    final boolean multiple = BooleanUtils.toBoolean(attributes.getValue(MULTI));
                    lookupProperty(name, multiple);
                }
                break;
            case VAL:
                startValueRecording();
                break;
            case MIXIN:
                startMixinRecording();
                break;
            case PTYPE:
                startPrimaryTypeRecording();
                break;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        switch (localName) {
            case MERGE:
                updateNode();
                break;
            case PROP:
                setProperty();
                break;
            case VAL:
                stopValueRecording();
                break;
            case MIXIN:
                stopMixinRecording();
                break;
            case PTYPE:
                setPrimaryType();
        }
    }

    private void updateNode() throws SAXException {
        try {
            if (mixins != null) {
                for (NodeType nodeType : node.getMixinNodeTypes()) {
                    final String mixinName = nodeType.getName();
                    if (!mixins.contains(mixinName)) {
                        node.removeMixin(mixinName);
                    } else {
                        mixins.remove(mixinName);
                    }
                }
                for (String mixin : mixins) {
                    node.addMixin(mixin);
                }
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            if (value != null) {
                value.append(ch, start, length);
            }
            if (type != null) {
                type.append(ch, start, length);
            }
        } catch (IOException e) {
            throw new SAXException("Error while processing property value", e);
        }
    }


    private void startValueRecording() {
        if (values != null) {
            value = new BufferedStringValue(session, ValueFactoryImpl.getInstance());
        }
    }

    private void stopValueRecording() {
        if (value != null) {
            values.add(value);
            value = null;
        }
    }

    private void startMixinRecording() {
        if (mixins != null) {
            type = new BufferedStringValue(session, ValueFactoryImpl.getInstance());
        }
    }

    private void stopMixinRecording() throws SAXException {
        try {
            if (type != null) {
                mixins.add(type.getValue(PropertyType.STRING, session).getString());
                type = null;
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private void startPrimaryTypeRecording() {
        if (node != null) {
            type = new BufferedStringValue(session, ValueFactoryImpl.getInstance());
        }
    }

    private void setPrimaryType() throws SAXException {
        try {
            if (type != null) {
                node.setPrimaryType(type.getValue(PropertyType.STRING, session).getString());
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private void setProperty() throws SAXException {
        try {
            if (property != null) {
                if (multiple) {
                    Value[] values = new Value[this.values.size()];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = this.values.get(i).getValue(property.getType(), session);
                    }
                    if (property.isMultiple()) {
                        property.setValue(values);
                    } else {
                        log.debug("Reversing property override that changed multi-valued property" +
                                " into single-valued property");
                        String propName = property.getName();
                        property.remove();
                        node.setProperty(propName, values);
                    }
                } else {
                    final Value value;
                    if (values.isEmpty()) {
                        value = node.getSession().getValueFactory().createValue(StringUtils.EMPTY);
                    } else {
                        value = values.get(0).getValue(property.getType(), session);
                    }
                    if (property.isMultiple()) {
                        log.debug("Reversing property override that changed single-valued property" +
                                " into multi-valued property");
                        String propName = property.getName();
                        property.remove();
                        node.setProperty(propName, value);
                    } else {
                        property.setValue(value);
                    }
                }
                property = null;
                values = null;
                multiple = null;
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private void lookupProperty(final String name, final boolean multi) throws SAXException {
        try {
            if (node != null) {
                if (node.hasProperty(name)) {
                    property = node.getProperty(name);
                    values = new ArrayList<>();
                    multiple = multi;
                } else {
                    log.warn("Cannot undo property {}/{}: not found", node.getPath(), name);
                }
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private void removeProperty(final String name) throws SAXException {
        try {
            if (node != null) {
                if (node.hasProperty(name)) {
                    node.getProperty(name).remove();
                } else {
                    log.warn("Cannot remove property {}/{}: not found", node.getPath(), name);
                }
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private void lookupNode(final String uuid) throws SAXException {
        try {
            node = session.getNodeByIdentifier(uuid);
            mixins = new ArrayList<>();
        } catch (ItemNotFoundException e) {
            log.warn("Cannot undo merge of node {}: not found", uuid);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private void removeNode(final String uuid) throws SAXException {
        try {
            session.getNodeByIdentifier(uuid).remove();
        } catch (ItemNotFoundException e) {
            log.warn("Cannot remove node {}: not found", uuid);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }
}
