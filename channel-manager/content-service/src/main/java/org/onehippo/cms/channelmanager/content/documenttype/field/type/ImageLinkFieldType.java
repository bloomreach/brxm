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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.ckeditor.Json;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageLinkFieldType extends PrimitiveFieldType implements NodeFieldType {

    public static final Logger log = LoggerFactory.getLogger(ImageLinkFieldType.class);

    private static final String[] IMAGE_PICKER_STRING_PROPERTIES = {
            "base.uuid",
            "cluster.name",
            "enable.upload",
            "last.visited.enabled",
            "last.visited.key",
            "preview.width"
    };
    private static final String[] IMAGE_PICKER_MULTIPLE_STRING_PROPERTIES = {
            "nodetypes",
    };

    private static final String IMAGE_PICKER_PROPERTY_PREFIX = "image.";
    private static final String[] IMAGE_PICKER_PREFIXED_STRING_PROPERTIES = {
            IMAGE_PICKER_PROPERTY_PREFIX + "validator.id"
    };

    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    private static final ImageItemFactory IMAGE_ITEM_FACTORY = new ImageItemFactory();

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

        config = Json.object();

        final ObjectNode imagePickerConfig = new FieldTypeConfig(fieldContext)
                .strings(IMAGE_PICKER_STRING_PROPERTIES)
                .multipleStrings(IMAGE_PICKER_MULTIPLE_STRING_PROPERTIES)
                .removePrefix(IMAGE_PICKER_PROPERTY_PREFIX)
                .strings(IMAGE_PICKER_PREFIXED_STRING_PROPERTIES)
                .build();
        config.set("imagepicker", imagePickerConfig);
    }

    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
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
            log.warn("Failed to read nodes for image link type '{}'", getId(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public FieldValue readValue(final Node node) {
        final FieldValue value = new FieldValue();
        try {
            final String uuid = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_DOCBASE, null);
            value.setValue(uuid);
            value.setId(node.getIdentifier());
            value.setUrl(createImageUrl(uuid, node.getSession()));
        } catch (final RepositoryException e) {
            log.warn("Failed to read image link '{}' from node '{}'", getId(), JcrUtils.getNodePathQuietly(node), e);
        }
        return value;
    }

    private String createImageUrl(final String uuid, final Session session) {
        final ImageItem imageItem = IMAGE_ITEM_FACTORY.createImageItem(uuid);
        return imageItem.getPrimaryUrl(() -> session);
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException {
        // TODO
    }

    @Override
    public boolean writeFieldValue(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException, RepositoryException {
        // TODO
        return false;
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        // TODO
        return false;
    }
}
