/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LinkFieldType extends PrimitiveFieldType implements NodeFieldType {

    private static final Logger log = LoggerFactory.getLogger(LinkFieldType.class);

    private static final String DEFAULT_VALUE = StringUtils.EMPTY;

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

    @Override
    protected void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean validateValues) throws ErrorWithPayloadException {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        if (validateValues) {
            checkCardinality(values);
        }

        try {
            final NodeIterator children = node.getNodes(valueName);
            FieldTypeUtils.writeNodeValues(children, values, getMaxValues(), this);
        } catch (final RepositoryException e) {
            log.warn("Failed to write {} field '{}'", getType(), valueName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    @Override
    protected List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();

        try {
            final NodeIterator children = node.getNodes(nodeName);
            final List<FieldValue> values = new ArrayList<>((int) children.getSize());
            for (final Node child : new NodeIterable(children)) {
                final FieldValue value = readValue(child);
                if (value.hasValue()) {
                    values.add(value);
                }
            }
            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read nodes for {} type '{}'", getType(), getId(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public FieldValue readValue(final Node node) {
        final FieldValue value = new FieldValue();
        try {
            final String uuid = readUuid(node);
            value.setValue(uuid);
            value.setMetadata(createMetadata(uuid, node, node.getSession()));
        } catch (final RepositoryException e) {
            log.warn("Failed to read {} '{}' from node '{}'", getType(), getId(), JcrUtils.getNodePathQuietly(node), e);
        }
        return value;
    }

    protected Map<String, Object> createMetadata(final String uuid, final Node node, final Session session) throws RepositoryException {
        return null;
    }

    private static String readUuid(final Node node) throws RepositoryException {
        final String uuid = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_DOCBASE, StringUtils.EMPTY);
        final String rootUuid = node.getSession().getRootNode().getIdentifier();
        return uuid.equals(rootUuid) ? StringUtils.EMPTY : uuid;
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws RepositoryException {
        writeUuid(node, fieldValue.getValue());
    }

    private static void writeUuid(final Node node, final String uuid) throws RepositoryException {
        if (StringUtils.isEmpty(uuid)) {
            final String rootUuid = node.getSession().getRootNode().getIdentifier();
            writeDocBase(node, rootUuid);
        } else {
            writeDocBase(node, uuid);
        }
    }

    private static void writeDocBase(final Node node, final String rootUuid) throws RepositoryException {
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, rootUuid);
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        return !isRequired() || validateSingleRequired(value);
    }

}
