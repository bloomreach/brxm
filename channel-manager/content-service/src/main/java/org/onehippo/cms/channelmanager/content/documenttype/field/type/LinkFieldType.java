/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LinkFieldType extends NodeFieldType implements LeafFieldType {

    private static final Logger log = LoggerFactory.getLogger(LinkFieldType.class);

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
}
