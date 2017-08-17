/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.ckeditor.Json;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImageLinkFieldType extends AbstractFieldType {

    public static final Logger log = LoggerFactory.getLogger(ImageLinkFieldType.class);

    private static final String[] IMAGEPICKER_STRING_PROPERTIES = {
        "nodetypes",
        "cluster.name",
        "enable.upload"
    };

    private ObjectNode config;

    public ImageLinkFieldType() {
        setType(Type.IMAGE_LINK);
    }

    public ObjectNode getConfig() {
        return config;
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        try {
            config = Json.object("{}");
        } catch (IOException e) {
            log.warn("Error while reading config of image link field '{}'", getId(), e);
        }

        final ObjectNode imagePickerConfig = getConfig().with("imagepicker");
        readStringConfig(imagePickerConfig, IMAGEPICKER_STRING_PROPERTIES, fieldContext);
    }

    private void readStringConfig(final ObjectNode config, final String[] propertyNames, final FieldTypeContext fieldContext) {
        for (final String propertyName : propertyNames) {
            fieldContext.getStringConfig(propertyName).ifPresent((value) -> config.put(propertyName, value));
        }
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    @Override
    protected void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean validateValues) throws ErrorWithPayloadException {
        log.info("Write image-link value");
    }

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> value) throws ErrorWithPayloadException {
        return false;
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        return false;
    }

    private List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();

        try {
            final NodeIterator children = node.getNodes(nodeName);
            final List<FieldValue> values = new ArrayList<>((int)children.getSize());
            for (final Node child : new NodeIterable(children)) {
                final FieldValue value = readValue(child);
                if (value.hasValue()) {
                    values.add(value);
                }
            }
            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read nodes for image link type '{}'", getId(), e);
        }
        return Collections.emptyList();
    }

    private FieldValue readValue(final Node node) {
        final FieldValue value = new FieldValue();
        try {
            final String docBase = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_DOCBASE, null);
            if (docBase != null) {
                final Session session = node.getSession();
                value.setValue(session.getNodeByIdentifier(docBase).getPath());
            }
            value.setId(node.getIdentifier());
        } catch (final RepositoryException e) {
            log.warn("Failed to read image link '{}' from node '{}'", getId(), JcrUtils.getNodePathQuietly(node), e);
        }
        return value;
    }
}
