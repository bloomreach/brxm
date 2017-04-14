/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.processor.richtext.RichTextNodeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextFieldType extends FormattedTextFieldType implements NodeFieldType {

    private static final RichTextNodeProcessor RICH_TEXT_NODE_PROCESSOR = new RichTextNodeProcessor();
    private static final Logger log = LoggerFactory.getLogger(RichTextFieldType.class);

    public RichTextFieldType() {
        super(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG);
    }

    @Override
    public void init(final FieldTypeContext fieldContext, final String choiceId) {
        throw new UnsupportedOperationException("The frontend expects that chosen rich text fields are always wrapped in a compound");
    }

    @Override
    public void init(final ContentTypeContext parentContext, final String choiceId) {
        setId(choiceId);
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    protected List<FieldValue> readValues(final Node node) {
        try {
            final NodeIterator children = node.getNodes(getId());
            final List<FieldValue> values = new ArrayList<>((int)children.getSize());
            for (final Node child : new NodeIterable(children)) {
                final FieldValue value = readValue(child);
                if (value.isPresent()) {
                    values.add(value);
                }
            }
            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read rich text field '{}'", getId(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public FieldValue readValue(final Node node) {
        final FieldValue value = new FieldValue();
        try {
            final String storedHtml = JcrUtils.getStringProperty(node, HippoStdNodeType.HIPPOSTD_CONTENT, null);
            final String processedHtml = RICH_TEXT_NODE_PROCESSOR.read(storedHtml, node, "richtext");
            value.setValue(processedHtml);
        } catch (final RepositoryException e) {
            log.warn("Failed to read rich text field '{}' from node '{}'", getId(), JcrUtils.getNodePathQuietly(node), e);
        }
        return value;
    }

    @Override
    public void writeTo(final Node node, final Optional<List<FieldValue>> optionalValues) throws ErrorWithPayloadException {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());
        checkCardinality(values);

        try {
            final NodeIterator children = node.getNodes(valueName);
            FieldTypeUtils.writeNodeValues(children, values, getMaxValues(), this);
        } catch (final RepositoryException e) {
            log.warn("Failed to write rich text field '{}'", valueName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException {
        final RichTextNodeProcessor processor = new RichTextNodeProcessor();
        final String html = processor.write(fieldValue.getValue(), node, "richtext");
        node.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, html);
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        return validateSingleRequired(value);
    }
}
